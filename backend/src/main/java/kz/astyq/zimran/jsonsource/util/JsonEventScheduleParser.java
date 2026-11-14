package kz.astyq.zimran.jsonsource.util;

import kz.astyq.zimran.event.model.domain.EventSchedule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static kz.astyq.zimran.jsonsource.util.JsonEventNormalizeUtils.scalar;
import static kz.astyq.zimran.jsonsource.util.JsonEventNormalizeUtils.strings;
import static kz.astyq.zimran.jsonsource.util.JsonEventNormalizeUtils.text;

/**
 * Reads the days of a multi-day or repeating event from a feed record. Fields (all optional, invalid entries
 * are skipped one by one, nothing is guessed):
 * <pre>
 * startDate, endDate        ISO dates, both inclusive
 * recurrenceDays            days of the week: 0-6 with 0 = Sunday (7 also = Sunday), or names ("monday", "mon")
 * recurrenceMonths          months 1-12
 * occurrenceDates           ISO dates: the only days the event is on
 * availableDateRanges       [[from, to], ...] or [{start, end}, ...]: the event is on only within these
 * excludedDates             ISO dates the event is not on
 * </pre>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonEventScheduleParser {

    /**
     * The schedule of an event that is on more than one day, or null for a single-day or undated record (and
     * for an inconsistent one whose endDate is before its startDate).
     */
    public static EventSchedule parse(JsonNode record) {
        LocalDate first = date(text(record, "startDate"));
        LocalDate last = date(text(record, "endDate"));
        if (first != null && last != null && last.isBefore(first)) {
            return null;
        }
        Set<DayOfWeek> days = daysOfWeek(record.get("recurrenceDays"));
        Set<Month> months = months(record.get("recurrenceMonths"));
        Set<LocalDate> dates = dates(record.get("occurrenceDates"));
        List<EventSchedule.DateRange> ranges = ranges(record.get("availableDateRanges"));
        boolean repeats = !days.isEmpty() || !dates.isEmpty() || !ranges.isEmpty();
        boolean multiDay = first != null && last != null && last.isAfter(first);
        if (!repeats && !multiDay) {
            return null;
        }
        if (first == null) {
            // The earliest listed day is the first one; an open weekly schedule keeps no lower bound.
            first = dates.stream().min(Comparator.naturalOrder())
                    .or(() -> ranges.stream().map(EventSchedule.DateRange::first).min(Comparator.naturalOrder()))
                    .orElse(null);
        }
        return EventSchedule.builder()
                .firstDate(first)
                .lastDate(last)
                .daysOfWeek(days)
                .months(months)
                .dates(dates)
                .ranges(ranges)
                .excludedDates(dates(record.get("excludedDates")))
                .build();
    }

    /**
     * The day of a record whose startDate (with no or the same endDate) is its only day, or null.
     */
    public static LocalDate singleDay(JsonNode record) {
        LocalDate first = date(text(record, "startDate"));
        LocalDate last = date(text(record, "endDate"));
        return first != null && (last == null || last.equals(first)) ? first : null;
    }

    private static Set<DayOfWeek> daysOfWeek(JsonNode value) {
        Set<DayOfWeek> days = new LinkedHashSet<>();
        for (String raw : strings(value)) {
            DayOfWeek day = dayOfWeek(raw);
            if (day != null) {
                days.add(day);
            }
        }
        return days;
    }

    private static DayOfWeek dayOfWeek(String raw) {
        try {
            int number = Integer.parseInt(raw);
            if (number == 0 || number == 7) {
                return DayOfWeek.SUNDAY;
            }
            return number >= 1 && number <= 6 ? DayOfWeek.of(number) : null;
        } catch (NumberFormatException e) {
            String name = raw.toUpperCase(Locale.ROOT);
            for (DayOfWeek day : DayOfWeek.values()) {
                if (name.length() >= 3 && day.name().startsWith(name)) {
                    return day;
                }
            }
            return null;
        }
    }

    private static Set<Month> months(JsonNode value) {
        Set<Month> months = new LinkedHashSet<>();
        for (String raw : strings(value)) {
            try {
                int number = Integer.parseInt(raw);
                if (number >= 1 && number <= 12) {
                    months.add(Month.of(number));
                }
            } catch (NumberFormatException ignored) {
                // not a month number
            }
        }
        return months;
    }

    private static Set<LocalDate> dates(JsonNode value) {
        Set<LocalDate> dates = new LinkedHashSet<>();
        strings(value).stream().map(JsonEventScheduleParser::date).filter(Objects::nonNull).forEach(dates::add);
        return dates;
    }

    private static List<EventSchedule.DateRange> ranges(JsonNode value) {
        List<EventSchedule.DateRange> ranges = new ArrayList<>();
        if (value == null || !value.isArray()) {
            return ranges;
        }
        for (int i = 0; i < value.size(); i++) {
            JsonNode item = value.get(i);
            LocalDate from;
            LocalDate to;
            if (item.isArray() && item.size() == 2) {
                from = date(scalar(item.get(0)));
                to = date(scalar(item.get(1)));
            } else if (item.isObject()) {
                from = date(text(item, "start", "from", "startDate"));
                to = date(text(item, "end", "to", "endDate"));
            } else {
                continue;
            }
            if (from != null && to != null && !to.isBefore(from)) {
                ranges.add(new EventSchedule.DateRange(from, to));
            }
        }
        return ranges;
    }

    private static LocalDate date(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value.strip());
        } catch (DateTimeException e) {
            return null;
        }
    }
}
