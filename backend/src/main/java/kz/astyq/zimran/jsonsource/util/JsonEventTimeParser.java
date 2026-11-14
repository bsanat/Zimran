package kz.astyq.zimran.jsonsource.util;

import kz.astyq.zimran.jsonsource.model.domain.ParsedStart;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Reads event start values. Accepted, in this order:
 * <ul>
 *   <li>ISO date-time with offset or {@code Z} ({@code 2026-11-20T20:00:00-05:00}): converted to the event zone;</li>
 *   <li>ISO local date-time ({@code 2026-11-20T20:00}): read in the event zone;</li>
 *   <li>ISO date ({@code 2026-11-20}), optionally with a separate local time ({@code 20:00}).</li>
 * </ul>
 * Anything else is unknown; the caller keeps the raw text as {@code dateText}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonEventTimeParser {

    public static ParsedStart parse(String start, String date, String time, ZoneId zone) {
        ParsedStart fromStart = parseStart(start, zone);
        if (fromStart.getLocalDate() != null && (fromStart.getStartAt() != null || time == null)) {
            return fromStart;
        }
        LocalDate day = fromStart.getLocalDate() != null ? fromStart.getLocalDate() : localDate(date);
        if (day == null) {
            return ParsedStart.UNKNOWN;
        }
        LocalTime localTime = localTime(time);
        if (localTime == null) {
            return ParsedStart.builder().localDate(day).build();
        }
        OffsetDateTime startAt = day.atTime(localTime).atZone(zone).toOffsetDateTime();
        return ParsedStart.builder().startAt(startAt).localDate(startAt.toLocalDate()).build();
    }

    private static ParsedStart parseStart(String value, ZoneId zone) {
        if (value == null) {
            return ParsedStart.UNKNOWN;
        }
        try {
            OffsetDateTime startAt = OffsetDateTime.parse(value).atZoneSameInstant(zone).toOffsetDateTime();
            return ParsedStart.builder().startAt(startAt).localDate(startAt.toLocalDate()).build();
        } catch (DateTimeParseException ignored) {
            // not an offset date-time
        }
        try {
            OffsetDateTime startAt = LocalDateTime.parse(value).atZone(zone).toOffsetDateTime();
            return ParsedStart.builder().startAt(startAt).localDate(startAt.toLocalDate()).build();
        } catch (DateTimeParseException ignored) {
            // not a local date-time
        }
        LocalDate day = localDate(value);
        return day != null ? ParsedStart.builder().localDate(day).build() : ParsedStart.UNKNOWN;
    }

    private static LocalDate localDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeException e) {
            return null;
        }
    }

    private static LocalTime localTime(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeException e) {
            return null;
        }
    }
}
