package kz.astyq.zimran.event.model.domain;

import kz.astyq.zimran.event.model.enums.CategorySlug;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.model.enums.Interest;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Set;

/**
 * Normalized event. Every value comes from the event source; optional fields are null when the source did not
 * send them (nothing is guessed). {@code description}, {@code title} and {@code venue} are untrusted text.
 */
@Value
@Builder
public class Event {

    @NonNull
    EventId id;
    @NonNull
    String title;
    /** Primary interest; always one of {@link #interests}. */
    @NonNull
    Interest category;
    /** Every interest the source categories map to. */
    @NonNull
    Set<Interest> interests;
    @NonNull
    String city;
    /** State code of the city (e.g. NY); null when the source does not say. */
    String state;
    @NonNull
    ZoneId timezone;
    @NonNull
    EventStatus status;

    /** Start in {@link #timezone}; null when the source has no start time. */
    OffsetDateTime startAt;
    /**
     * Day of the event in {@link #timezone}; set whenever the date is known, also when the time is not. Equals
     * the date of {@link #startAt} when that is set.
     */
    LocalDate localDate;
    /** Raw date text from the source, shown when {@link #startAt} is null. */
    String dateText;
    /**
     * Days of an event that spans several days or repeats; null for a single-day event. When set, it decides
     * which days the event is on ({@link #startAt}, if any, is the first occurrence and gives the time of day).
     */
    EventSchedule schedule;

    String venue;
    String address;
    String description;
    String imageUrl;
    String sourceUrl;

    /** Null when unknown; never 0 unless the source says so (free). */
    BigDecimal priceMin;
    BigDecimal priceMax;
    String currency;
    boolean priceKnown;
    /** Raw price text from the source. */
    String priceText;

    boolean major;

    /** A fictional sample event ({@code app.demo.enabled}); the frontend labels it "Sample / Demo". */
    boolean demo;

    /**
     * The shared category of the event: the slug of its primary interest.
     */
    public CategorySlug getCategorySlug() {
        return category.getCategorySlug();
    }

    /**
     * Day of the event in {@code zone}: the start instant's day there when the time is known, else
     * {@link #localDate} (a date without time has no instant to convert). Null when the date is unknown.
     */
    public LocalDate dateIn(ZoneId zone) {
        if (startAt != null) {
            return startAt.atZoneSameInstant(zone).toLocalDate();
        }
        return localDate;
    }

    /**
     * Whether the source says on which day(s) the event happens.
     */
    public boolean isDated() {
        return schedule != null || startAt != null || localDate != null;
    }

    /**
     * First day in {@code from}..{@code to} (both inclusive, either may be null = open) on which the event
     * happens, as a day in {@code zone}; null when it happens on none of them or its date is unknown. A
     * scheduled event's days are days in its own timezone and are compared as they are.
     */
    public LocalDate firstDayBetween(LocalDate from, LocalDate to, ZoneId zone) {
        if (schedule != null) {
            return schedule.firstDayBetween(from, to);
        }
        LocalDate day = dateIn(zone);
        if (day == null || (from != null && day.isBefore(from)) || (to != null && day.isAfter(to))) {
            return null;
        }
        return day;
    }
}
