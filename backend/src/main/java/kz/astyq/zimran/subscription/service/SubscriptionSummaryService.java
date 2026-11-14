package kz.astyq.zimran.subscription.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.subscription.model.dto.SubscriptionViewResponse;

/**
 * The caller's subscription as shown to the client: status, plan and remaining limits.
 */
public interface SubscriptionSummaryService {

    SubscriptionViewResponse getSummary(RequestScope scope);
}
