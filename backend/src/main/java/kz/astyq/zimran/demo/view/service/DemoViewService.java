package kz.astyq.zimran.demo.view.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.demo.model.dto.DemoAlertCheckViewResponse;
import kz.astyq.zimran.demo.model.dto.DemoEventCreateRequest;
import kz.astyq.zimran.event.model.dto.EventDetailsViewResponse;
import kz.astyq.zimran.subscription.model.dto.SubscriptionViewResponse;

public interface DemoViewService {

    /**
     * "Try Premium Demo": gives the caller demo Premium and returns the updated subscription.
     */
    SubscriptionViewResponse activatePremium(RequestScope scope);

    /**
     * Adds one fictional sample event to the catalog (caller must be {@code DEMO_PREMIUM}; rate limited).
     */
    EventDetailsViewResponse createEvent(DemoEventCreateRequest request, RequestScope scope);

    /**
     * Runs alert matching for the caller over demo events not yet notified, delivering immediately (caller must be
     * {@code DEMO_PREMIUM}; rate limited).
     */
    DemoAlertCheckViewResponse checkAlerts(RequestScope scope);
}
