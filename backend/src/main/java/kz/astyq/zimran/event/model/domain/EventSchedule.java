package kz.astyq.zimran.event.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Set;

/**
 * Days on which an event that spans several days takes place (an exhibition, a festival, a weekly tour). Every
 * part is optional and the parts combine: a day counts when it is inside {@link #firstDate}..{@link #lastDate},
 * is one of {@link #dates} (when listed), inside one of {@link #ranges} (when listed), on one of
 * {@link #daysOfWeek} (when listed), in one of {@link #months} (when listed), and not in {@link #excludedDates}.
 * Dates are days in the event's own timezone.
 */
@Value
@Builder
@Jacksonized
public class EventSchedule {

    /** How far ahead an open-ended schedule is searched for its next day. */
    static final int MAX_LOOKAHEAD_DAYS = 400;

    /** First day; null = no lower bound. */
    LocalDate firstDate;
    /** Last day, inclusive; null = no upper bound. */
    LocalDate lastDate;
    /** Empty = no restriction. */
    @NonNull
    @Singular("dayOfWeek")
    Set<DayOfWeek> daysOfWeek;
    /** Empty = no restriction. */
    @NonNull
    @Singular
    Set<Month> months;
    /** The only days the event happens on; empty = no restriction. */
    @NonNull
    @Singular
    Set<LocalDate> dates;
    /** Inclusive date ranges the event happens in; empty = no restriction. */
    @NonNull
    @Singular
    List<DateRange> ranges;
    @NonNull
    @Singular
    Set<LocalDate> excludedDates;

    public boolean occursOn(LocalDate day) {
        if (day == null
                || (firstDate != null && day.isBefore(firstDate))
                || (lastDate != null && day.isAfter(lastDate))
                || excludedDates.contains(day)) {
            return false;
        }
        return (dates.isEmpty() || dates.contains(day))
                && (ranges.isEmpty() || ranges.stream().anyMatch(range -> range.contains(day)))
                && (daysOfWeek.isEmpty() || daysOfWeek.contains(day.getDayOfWeek()))
                && (months.isEmpty() || months.contains(day.getMonth()));
    }

    /**
     * First day on or after {@code from} (null = {@link #firstDate}, else no lower bound) and on or before
     * {@code to} (null = open) on which the event happens, or null when there is none. An open search stops
     * {@value #MAX_LOOKAHEAD_DAYS} days after its start.
     */
    public LocalDate firstDayBetween(LocalDate from, LocalDate to) {
        LocalDate start = from;
        if (firstDate != null && (start == null || start.isBefore(firstDate))) {
            start = firstDate;
        }
        if (start == null) {
            return null;
        }
        LocalDate end = to;
        if (lastDate != null && (end == null || end.isAfter(lastDate))) {
            end = lastDate;
        }
        LocalDate limit = start.plusDays(MAX_LOOKAHEAD_DAYS);
        if (end == null || end.isAfter(limit)) {
            end = limit;
        }
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            if (occursOn(day)) {
                return day;
            }
        }
        return null;
    }

    /**
     * Inclusive range of days.
     */
    public record DateRange(@NonNull LocalDate first, @NonNull LocalDate last) {

        public boolean contains(LocalDate day) {
            return !day.isBefore(first) && !day.isAfter(last);
        }
    }
}
