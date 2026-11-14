package kz.astyq.zimran.notification.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.event.model.domain.StoredEvent;
import kz.astyq.zimran.notification.model.domain.DeliveryWindow;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Creates {@code NEW_EVENT_MATCH} notifications (with their in-app delivery). Matching is the caller's job.
 */
public interface NotificationCreationService {

    /**
     * Creates the scope's notification for the event unless it already has one (insert-or-ignore).
     *
     * @return the public id when created; empty when it already existed
     */
    Optional<String> createIfAbsent(RequestScope scope, StoredEvent event, DeliveryWindow window);

    /**
     * Catalog ids among {@code catalogIds} the scope already has a notification for.
     */
    Set<Long> findNotifiedEventIds(RequestScope scope, Collection<Long> catalogIds);
}
