package kz.astyq.zimran.event.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Event message keys in {@code messages*.properties}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageCode {

    /** Argument {0}: the requested event id. */
    public static final String EVENT_NOT_FOUND = "event_not_found";
    public static final String DATE_RANGE_INVALID = "date_range_invalid";
    /** Argument {0}: the maximum number of days. */
    public static final String DATE_RANGE_TOO_LONG = "date_range_too_long";
    public static final String DATE_IN_PAST = "date_in_past";
}
