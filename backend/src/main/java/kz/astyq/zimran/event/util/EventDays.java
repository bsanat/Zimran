package kz.astyq.zimran.event.util;

import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventSchedule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * First and last day an event can happen on, in its own timezone. Stored with each catalog row to pre-filter date
 * ranges in SQL and to expire past events; null means open (undated, or a schedule without that bound).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventDays {

    public static LocalDate firstDay(Event event) {
        EventSchedule schedule = event.getSchedule();
        if (schedule == null) {
            return event.dateIn(event.getTimezone());
        }
        LocalDate listed = listedDays(schedule).min(Comparator.naturalOrder()).orElse(null);
        return latest(schedule.getFirstDate(), listed);
    }

    public static LocalDate lastDay(Event event) {
        EventSchedule schedule = event.getSchedule();
        if (schedule == null) {
            return event.dateIn(event.getTimezone());
        }
        LocalDate listed = listedDays(schedule).max(Comparator.naturalOrder()).orElse(null);
        return earliest(schedule.getLastDate(), listed);
    }

    /**
     * Days that bound the schedule: its explicit dates when it lists any, else the ends of its ranges, else
     * nothing. (Dates and ranges combine with AND, so either one alone already bounds the days.)
     */
    private static Stream<LocalDate> listedDays(EventSchedule schedule) {
        if (!schedule.getDates().isEmpty()) {
            return schedule.getDates().stream();
        }
        return schedule.getRanges().stream().flatMap(range -> Stream.of(range.first(), range.last()));
    }

    private static LocalDate latest(LocalDate a, LocalDate b) {
        if (a == null) {
            return b;
        }
        return b == null || a.isAfter(b) ? a : b;
    }

    private static LocalDate earliest(LocalDate a, LocalDate b) {
        if (a == null) {
            return b;
        }
        return b == null || a.isBefore(b) ? a : b;
    }
}
