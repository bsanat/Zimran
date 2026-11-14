package kz.astyq.zimran.notification.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.notification.model.domain.NotificationCheckResult;

import java.util.List;

/**
 * Matches new catalog events against alert preferences and creates notifications. Candidates are Premium scopes
 * (via {@code EntitlementService}) with active preferences in the event's city; see {@code AlertMatchRules} for the
 * per-event rules. Creation is insert-or-ignore, so running twice never notifies twice.
 */
public interface NotificationMatchingService {

    /**
     * Matches events that ingestion just inserted. Demo samples are skipped: only {@link #matchDemoEvents} handles
     * them. Delivery follows each scope's frequency.
     *
     * @return the number of notifications created
     */
    int match(List<Event> newEvents);

    /**
     * Demo only: matches the demo sample events not yet notified to {@code scope}, with the real rules, and
     * delivers immediately whatever the scope's frequency. Callers check that the scope may use the demo.
     */
    NotificationCheckResult matchDemoEvents(RequestScope scope);
}
