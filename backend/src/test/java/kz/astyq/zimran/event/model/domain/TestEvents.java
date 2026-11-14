package kz.astyq.zimran.event.model.domain;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.model.enums.Interest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumSet;

/**
 * Test-only event fixtures: a scheduled New York music event with only the required fields set.
 */
public final class TestEvents {

    public static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    public static final SupportedCity NYC = SupportedCity.builder()
            .id("nyc").name("New York").stateCode("NY").countryCode("US").zone(NEW_YORK).build();

    private TestEvents() {
    }

    public static Event.EventBuilder event(String sourceId) {
        return Event.builder()
                .id(new EventId(EventSource.JSON, sourceId))
                .title("Event " + sourceId)
                .category(Interest.MUSIC)
                .interests(EnumSet.of(Interest.MUSIC))
                .city("New York")
                .timezone(NEW_YORK)
                .status(EventStatus.SCHEDULED);
    }

    /** {@link #event} starting at {@code date time} in New York. */
    public static Event.EventBuilder eventAt(String sourceId, LocalDate date, String time) {
        OffsetDateTime start = at(date, time);
        return event(sourceId).startAt(start).localDate(date);
    }

    public static OffsetDateTime at(LocalDate date, String time) {
        return LocalDateTime.of(date, LocalTime.parse(time)).atZone(NEW_YORK).toOffsetDateTime();
    }

    /** Sets a known USD price from {@code min} to {@code max}. */
    public static Event.EventBuilder price(Event.EventBuilder builder, String min, String max) {
        return builder.priceMin(new BigDecimal(min)).priceMax(max != null ? new BigDecimal(max) : null)
                .currency("USD").priceKnown(true);
    }

    public static Event.EventBuilder interests(Event.EventBuilder builder, Interest first, Interest... more) {
        return builder.category(first).interests(EnumSet.of(first, more));
    }
}
