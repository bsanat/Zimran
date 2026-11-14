package kz.astyq.zimran.ticketmaster.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.ticketmaster.model.dto.TmClassification;
import kz.astyq.zimran.ticketmaster.model.dto.TmDates;
import kz.astyq.zimran.ticketmaster.model.dto.TmEvent;
import kz.astyq.zimran.ticketmaster.model.dto.TmStart;
import kz.astyq.zimran.ticketmaster.model.dto.TmVenue;
import kz.astyq.zimran.ticketmaster.service.InterestMappingService;
import kz.astyq.zimran.ticketmaster.service.TicketmasterEventNormalizer;
import kz.astyq.zimran.ticketmaster.util.TicketmasterNormalizeUtils;
import kz.astyq.zimran.ticketmaster.util.TicketmasterNormalizeUtils.MergedPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketmasterEventNormalizerImpl implements TicketmasterEventNormalizer {

    static final String DEFAULT_CATEGORY = "Other";
    /** Ticketmaster's placeholder segment name. */
    private static final String UNDEFINED = "Undefined";

    private final InterestMappingService interestMappingService;

    @Override
    public Optional<Event> normalize(TmEvent tm, SupportedCity city) {
        if (tm == null) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(tm.getId())) {
            return drop(tm, "missing_id");
        }
        if (!StringUtils.hasText(tm.getName())) {
            return drop(tm, "missing_title");
        }
        if (!StringUtils.hasText(tm.getUrl())) {
            return drop(tm, "missing_url");
        }
        if (!TicketmasterNormalizeUtils.isHttpsUrl(tm.getUrl())) {
            return drop(tm, "invalid_url");
        }

        TmDates dates = tm.getDates();
        TmVenue venue = firstVenue(tm);
        ZoneId zone = TicketmasterNormalizeUtils.firstValidZone(city.getZone(),
                dates != null ? dates.getTimezone() : null,
                venue != null ? venue.getTimezone() : null);
        Optional<OffsetDateTime> startAt = startAt(dates != null ? dates.getStart() : null, zone);
        if (startAt.isEmpty()) {
            return drop(tm, "missing_start");
        }

        Optional<MergedPrice> price = TicketmasterNormalizeUtils.mergePrices(tm.getPriceRanges());
        String venueCity = venue != null && venue.getCity() != null ? trimToNull(venue.getCity().getName()) : null;

        Event event = Event.builder()
                .id(new EventId(EventSource.TICKETMASTER, tm.getId().trim()))
                .title(tm.getName().trim())
                .sourceUrl(tm.getUrl().trim())
                .startAt(startAt.get())
                .timezone(zone)
                .status(status(dates))
                .description(TicketmasterNormalizeUtils.cleanDescription(
                        tm.getDescription(), tm.getInfo(), tm.getPleaseNote()))
                .category(category(tm.getClassifications()))
                .interests(List.copyOf(interestMappingService.resolve(tm.getClassifications())))
                .venue(venue != null ? trimToNull(venue.getName()) : null)
                .address(venue == null ? null : TicketmasterNormalizeUtils.joinNonBlank(
                        venue.getAddress() != null ? venue.getAddress().getLine1() : null,
                        venueCity,
                        venue.getState() != null ? venue.getState().getStateCode() : null,
                        venue.getPostalCode()))
                .city(venueCity != null ? venueCity : city.getName())
                .priceMin(price.map(MergedPrice::getMin).orElse(null))
                .priceMax(price.map(MergedPrice::getMax).orElse(null))
                .currency(price.map(MergedPrice::getCurrency).orElse(null))
                .priceKnown(price.isPresent())
                .imageUrl(TicketmasterNormalizeUtils.chooseImageUrl(tm.getImages()))
                .build();
        return Optional.of(event);
    }

    /**
     * {@code dateTime} (UTC) shifted to the zone, else {@code localDate + localTime} in the zone. Empty
     * when Ticketmaster flags the date or time as not final, or only a date is known.
     */
    static Optional<OffsetDateTime> startAt(TmStart start, ZoneId zone) {
        if (start == null || isTrue(start.getDateTBD()) || isTrue(start.getTimeTBA())
                || isTrue(start.getNoSpecificTime())) {
            return Optional.empty();
        }
        if (StringUtils.hasText(start.getDateTime())) {
            try {
                return Optional.of(Instant.parse(start.getDateTime().trim()).atZone(zone).toOffsetDateTime());
            } catch (DateTimeParseException ignored) {
                // fall back to the local date and time
            }
        }
        if (StringUtils.hasText(start.getLocalDate()) && StringUtils.hasText(start.getLocalTime())) {
            try {
                LocalDate date = LocalDate.parse(start.getLocalDate().trim());
                LocalTime time = LocalTime.parse(start.getLocalTime().trim());
                return Optional.of(ZonedDateTime.of(date, time, zone).toOffsetDateTime());
            } catch (DateTimeParseException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    static EventStatus status(TmDates dates) {
        String code = dates != null && dates.getStatus() != null ? dates.getStatus().getCode() : null;
        if (code == null) {
            return EventStatus.SCHEDULED;
        }
        return switch (code.trim().toLowerCase(Locale.ROOT)) {
            case "cancelled", "canceled" -> EventStatus.CANCELLED;
            case "postponed", "rescheduled" -> EventStatus.POSTPONED;
            default -> EventStatus.SCHEDULED;
        };
    }

    /**
     * Segment of the primary classification (else of the first one); {@code Other} if none.
     */
    static String category(List<TmClassification> classifications) {
        if (classifications == null) {
            return DEFAULT_CATEGORY;
        }
        List<TmClassification> present = classifications.stream().filter(Objects::nonNull).toList();
        return present.stream()
                .filter(c -> isTrue(c.getPrimary()))
                .findFirst()
                .or(() -> present.stream().findFirst())
                .map(c -> c.getSegment() != null ? trimToNull(c.getSegment().getName()) : null)
                .filter(name -> !UNDEFINED.equalsIgnoreCase(name))
                .orElse(DEFAULT_CATEGORY);
    }

    private static TmVenue firstVenue(TmEvent tm) {
        if (tm.getEmbedded() == null || tm.getEmbedded().getVenues() == null) {
            return null;
        }
        return tm.getEmbedded().getVenues().stream().filter(Objects::nonNull).findFirst().orElse(null);
    }

    private static Optional<Event> drop(TmEvent tm, String reason) {
        if (log.isDebugEnabled()) {
            String id = tm.getId() != null ? tm.getId().replaceAll("[\\r\\n]", "_") : null;
            log.debug("Dropping Ticketmaster event {}: {}", id, reason);
        }
        return Optional.empty();
    }

    private static boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
