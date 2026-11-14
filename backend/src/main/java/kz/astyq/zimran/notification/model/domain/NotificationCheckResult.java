package kz.astyq.zimran.notification.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

/**
 * Outcome of a matching run for one scope.
 */
@Value
@Builder
public class NotificationCheckResult {

    public static final NotificationCheckResult EMPTY =
            NotificationCheckResult.builder().notificationIds(List.of()).build();

    /** Events examined (not yet notified to the scope). */
    int checked;
    /** Notifications created by this run. */
    int created;
    /** Public ids of the created notifications. */
    @NonNull
    List<String> notificationIds;
}
