package kz.astyq.zimran.notification.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Notification message keys in {@code messages*.properties}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageCode {

    public static final String NOTIFICATION_NOT_FOUND = "notification_not_found";

    /** Title of a {@code NEW_EVENT_MATCH} notification. Argument {0}: the event title. */
    public static final String NOTIFICATION_NEW_EVENT_MATCH = "notification_new_event_match";
}
