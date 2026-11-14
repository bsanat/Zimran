package kz.astyq.zimran.notification.model.domain;

import kz.astyq.zimran.notification.model.entity.Notification;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

/**
 * One page of a scope's delivered notifications, newest first.
 */
@Value
@Builder
public class NotificationInbox {

    @NonNull
    List<Notification> items;
    /** Delivered and unread, over all pages. */
    long unreadCount;
    boolean hasMore;
}
