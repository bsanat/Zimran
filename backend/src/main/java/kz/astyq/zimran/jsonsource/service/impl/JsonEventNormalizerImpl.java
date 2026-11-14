package kz.astyq.zimran.jsonsource.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.domain.EventSchedule;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.jsonsource.model.domain.ParsedStart;
import kz.astyq.zimran.jsonsource.service.CategoryMappingService;
import kz.astyq.zimran.jsonsource.service.JsonEventNormalizer;
import kz.astyq.zimran.jsonsource.util.JsonEventScheduleParser;
import kz.astyq.zimran.jsonsource.util.JsonEventTimeParser;
import kz.astyq.zimran.jsonsource.util.MessageCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static kz.astyq.zimran.jsonsource.util.JsonEventNormalizeUtils.amount;
import static kz.astyq.zimran.jsonsource.util.JsonEventNormalizeUtils.bool;
import static kz.astyq.zimran.jsonsource.util.JsonEventNormalizeUtils.cleanText;
import static kz.astyq.zimran.jsonsource.util.JsonEventNormalizeUtils.currency;
import static kz.astyq.zimran.jsonsource.util.JsonEventNormalizeUtils.hash;
import static kz.astyq.zimran.jsonsource.util.JsonEventNormalizeUtils.plainText;
import static kz.astyq.zimran.jsonsource.util.JsonEventNormalizeUtils.safeHttpsUrl;
import static kz.astyq.zimran.jsonsource.util.JsonEventNormalizeUtils.strings;
import static kz.astyq.zimran.jsonsource.util.JsonEventNormalizeUtils.text;
import static kz.astyq.zimran.jsonsource.util.JsonEventNormalizeUtils.urlSafeId;

/**
 * Normalizer for the <b>provisional</b> record format below (the real feed's format is not known yet; adjust the
 * field names here when it is). Every field is optional unless marked; the first listed alias wins.
 * <pre>
 * id | eventId | event_id                  stable id; missing = hash of title, start and venue
 * title | name                              REQUIRED
 * category | categories                     string or array; REQUIRED to map to an interest
 * city                                      missing = the default city; venue.city also read
 * state | stateCode                         state code; tells apart cities sharing a name; venue.state also read
 * timezone | timeZone                       IANA; missing or invalid = the city's timezone
 * startAt | start | startDateTime           ISO date-time (offset or local) or ISO date
 * date + time                               ISO date and local time, when there is no startAt
 * dateText                                  raw text shown when the start cannot be parsed
 * startDate, endDate, recurrenceDays, ...   days of a multi-day or repeating event, see JsonEventScheduleParser;
 *                                           a startDate alone (or equal to endDate) is the day when startAt is missing
 * venue                                     string, or object {name, address, city}
 * address                                   string or array of lines
 * description                               may hold HTML; reduced to plain text, 1000 characters
 * imageUrl | image | thumbnail              https only; when not https, imageSourceUrl (https) is used instead
 * sourceUrl | url | link                    https only
 * priceMin | minPrice, priceMax | maxPrice  numbers or numeric strings, >= 0; price (number) = both
 * price (string) | priceText                raw price text
 * isFree | free, or price text "Free"       price 0 (USD unless currency says otherwise)
 * currency                                  3 letters
 * status                                    scheduled/cancelled/postponed (+ aliases); missing = scheduled
 * isMajor | major                           boolean; missing = false
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JsonEventNormalizerImpl implements JsonEventNormalizer {

    static final int TITLE_MAX = 300;
    static final int TEXT_MAX = 300;
    static final int DESCRIPTION_MAX = 1000;
    static final String FREE_CURRENCY = "USD";

    private static final Map<String, EventStatus> STATUSES = Map.ofEntries(
            Map.entry("scheduled", EventStatus.SCHEDULED),
            Map.entry("active", EventStatus.SCHEDULED),
            Map.entry("onsale", EventStatus.SCHEDULED),
            Map.entry("on_sale", EventStatus.SCHEDULED),
            Map.entry("offsale", EventStatus.SCHEDULED),
            Map.entry("cancelled", EventStatus.CANCELLED),
            Map.entry("canceled", EventStatus.CANCELLED),
            Map.entry("postponed", EventStatus.POSTPONED),
            Map.entry("rescheduled", EventStatus.POSTPONED));

    private final CategoryMappingService categoryMappingService;
    private final SupportedCityService supportedCityService;

    @Override
    public List<Event> normalizeAll(JsonNode document) {
        JsonNode records = document != null && document.isObject() ? document.get("events") : document;
        if (records == null || !records.isArray()) {
            throw new ServiceValidationException(ErrorCode.UPSTREAM_UNAVAILABLE, MessageCode.EVENTS_SOURCE_UNAVAILABLE)
                    .withCause(new IllegalArgumentException("Expected an array of events or {\"events\": [...]}"));
        }
        List<Event> events = new ArrayList<>(records.size());
        Map<String, Integer> skipped = new LinkedHashMap<>();
        for (int i = 0; i < records.size(); i++) {
            JsonNode record = records.get(i);
            String reason = rejectReason(record);
            if (reason != null) {
                skipped.merge(reason, 1, Integer::sum);
                continue;
            }
            normalize(record).ifPresentOrElse(events::add, () -> skipped.merge("other", 1, Integer::sum));
        }
        if (!skipped.isEmpty()) {
            log.info("Event feed: {} of {} records skipped {}", records.size() - events.size(), records.size(), skipped);
        }
        return events;
    }

    @Override
    public Optional<Event> normalize(JsonNode record) {
        if (rejectReason(record) != null) {
            return Optional.empty();
        }
        String title = cleanText(text(record, "title", "name"), TITLE_MAX);
        List<Interest> interests = categoryMappingService.interestsFor(categories(record));
        EventStatus status = status(record);

        JsonNode venueNode = record.get("venue");
        boolean venueObject = venueNode != null && venueNode.isObject();
        String venue = cleanText(venueObject ? text(venueNode, "name") : text(record, "venue"), TEXT_MAX);
        String address = cleanText(address(record.get("address")), TEXT_MAX);
        if (address == null && venueObject) {
            address = cleanText(address(venueNode.get("address")), TEXT_MAX);
        }

        String rawCity = cleanText(text(record, "city"), TEXT_MAX);
        if (rawCity == null && venueObject) {
            rawCity = cleanText(text(venueNode, "city"), TEXT_MAX);
        }
        String rawState = cleanText(text(record, "state", "stateCode"), TEXT_MAX);
        if (rawState == null && venueObject) {
            rawState = cleanText(text(venueNode, "state", "stateCode"), TEXT_MAX);
        }
        Optional<SupportedCity> supportedCity = rawCity != null
                ? supportedCityService.findByName(rawCity, rawState) : Optional.of(supportedCityService.getDefault());
        String city = supportedCity.map(SupportedCity::getName).orElse(rawCity);
        String state = supportedCity.map(SupportedCity::getStateCode).filter(code -> !code.isBlank()).orElse(rawState);
        ZoneId zone = zone(text(record, "timezone", "timeZone"),
                supportedCity.orElseGet(supportedCityService::getDefault).getZone());

        String rawStart = text(record, "startAt", "start", "startDateTime");
        ParsedStart start = JsonEventTimeParser.parse(rawStart, text(record, "date"), text(record, "time"), zone);
        EventSchedule schedule = JsonEventScheduleParser.parse(record);
        LocalDate localDate = start.getLocalDate();
        if (localDate == null && schedule == null) {
            localDate = JsonEventScheduleParser.singleDay(record);
        }
        String dateText = cleanText(text(record, "dateText"), TEXT_MAX);
        if (dateText == null && start.getStartAt() == null) {
            // Without a start time the frontend can only show text: keep the source's own date value.
            dateText = cleanText(rawStart != null ? rawStart : text(record, "date"), TEXT_MAX);
        }

        String rawId = text(record, "id", "eventId", "event_id");
        String sourceEventId = rawId != null ? urlSafeId(rawId)
                : hash(title, String.valueOf(rawStart), String.valueOf(text(record, "date")), String.valueOf(venue));

        Price price = price(record);
        Interest category = interests.getFirst();
        Set<Interest> interestSet = EnumSet.copyOf(interests);
        return Optional.of(Event.builder()
                .id(new EventId(EventSource.JSON, sourceEventId))
                .title(title)
                .category(category)
                .interests(interestSet)
                .city(city)
                .state(state)
                .timezone(zone)
                .status(status)
                .startAt(start.getStartAt())
                .localDate(localDate)
                .dateText(dateText)
                .schedule(schedule)
                .venue(venue)
                .address(address)
                .description(plainText(text(record, "description"), DESCRIPTION_MAX))
                .imageUrl(imageUrl(record))
                .sourceUrl(safeHttpsUrl(text(record, "sourceUrl", "url", "link")))
                .priceMin(price.min)
                .priceMax(price.max)
                .priceKnown(price.min != null)
                .currency(price.currency)
                .priceText(cleanText(price.text, TEXT_MAX))
                .major(Boolean.TRUE.equals(bool(firstPresent(record, "isMajor", "major"))))
                .build());
    }

    /**
     * Why a record cannot become an event, or null when it can.
     */
    private String rejectReason(JsonNode record) {
        if (record == null || !record.isObject()) {
            return "notAnObject";
        }
        if (cleanText(text(record, "title", "name"), TITLE_MAX) == null) {
            return "noTitle";
        }
        if (categoryMappingService.interestsFor(categories(record)).isEmpty()) {
            return "unmappedCategory";
        }
        if (status(record) == null) {
            return "unknownStatus";
        }
        return null;
    }

    private static List<String> categories(JsonNode record) {
        List<String> categories = new ArrayList<>(strings(record.get("category")));
        categories.addAll(strings(record.get("categories")));
        return categories;
    }

    /**
     * Missing status = scheduled (the feed lists upcoming events); an unknown value = null (record skipped).
     */
    private static EventStatus status(JsonNode record) {
        String raw = text(record, "status");
        if (raw == null) {
            return EventStatus.SCHEDULED;
        }
        return STATUSES.get(raw.toLowerCase(Locale.ROOT).replace(' ', '_').replace("-", "_"));
    }

    /**
     * The record's image when it is an https URL; otherwise (e.g. a site-relative path) the https URL of the
     * image's original, when the record gives one.
     */
    private static String imageUrl(JsonNode record) {
        String image = safeHttpsUrl(text(record, "imageUrl", "image", "thumbnail"));
        return image != null ? image : safeHttpsUrl(text(record, "imageSourceUrl"));
    }

    private static String address(JsonNode value) {
        List<String> lines = strings(value);
        return lines.isEmpty() ? null : String.join(", ", lines);
    }

    private static ZoneId zone(String raw, ZoneId fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return ZoneId.of(raw);
        } catch (DateTimeException e) {
            return fallback;
        }
    }

    private static JsonNode firstPresent(JsonNode record, String... names) {
        for (String name : names) {
            JsonNode value = record.get(name);
            if (value != null && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    /**
     * Known only with a minimum price; an inconsistent range (min above max) counts as unknown. The text is kept
     * either way as a display fallback.
     */
    private static Price price(JsonNode record) {
        JsonNode priceNode = record.get("price");
        String text = text(record, "priceText");
        if (text == null && priceNode != null && priceNode.isString()) {
            text = text(record, "price");
        }
        String currency = currency(text(record, "currency"));
        if (Boolean.TRUE.equals(bool(firstPresent(record, "isFree", "free"))) || "free".equalsIgnoreCase(text)) {
            return new Price(BigDecimal.ZERO, BigDecimal.ZERO, currency != null ? currency : FREE_CURRENCY, text);
        }
        BigDecimal min = amount(firstPresent(record, "priceMin", "minPrice"));
        BigDecimal max = amount(firstPresent(record, "priceMax", "maxPrice"));
        if (min == null && max == null && priceNode != null && priceNode.isNumber()) {
            min = amount(priceNode);
            max = min;
        }
        if (min == null || (max != null && max.compareTo(min) < 0)) {
            return new Price(null, null, null, text);
        }
        return new Price(min, max, currency, text);
    }

    private static final class Price {
        private final BigDecimal min;
        private final BigDecimal max;
        private final String currency;
        private final String text;

        private Price(BigDecimal min, BigDecimal max, String currency, String text) {
            this.min = min;
            this.max = max;
            this.currency = currency;
            this.text = text;
        }
    }
}
