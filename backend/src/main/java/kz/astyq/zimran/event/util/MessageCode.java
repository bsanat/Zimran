package kz.astyq.zimran.event.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Event message keys in {@code messages*.properties}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageCode {

    public static final String EVENT_NOT_FOUND = "event_not_found";
    public static final String DATE_IN_PAST = "date_in_past";
    public static final String EVENTS_SOURCE_UNAVAILABLE = "events_source_unavailable";
}
