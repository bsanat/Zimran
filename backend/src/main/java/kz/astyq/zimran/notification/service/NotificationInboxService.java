package kz.astyq.zimran.notification.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.notification.model.domain.NotificationInbox;

/**
 * The scope's notification center. Only delivered notifications ({@code deliverAfter <= now}) exist for it.
 */
public interface NotificationInboxService {

    /**
     * One page (1-based) of delivered notifications, newest first.
     */
    NotificationInbox getPage(RequestScope scope, int page, int size);

    /**
     * Marks one of the scope's delivered notifications as read; idempotent. Unknown, not yet delivered or another
     * scope's: {@code 404 NOT_FOUND}.
     */
    void markRead(RequestScope scope, String notificationId);

    /**
     * Marks every delivered notification of the scope as read.
     *
     * @return how many were unread
     */
    int markAllRead(RequestScope scope);
}
