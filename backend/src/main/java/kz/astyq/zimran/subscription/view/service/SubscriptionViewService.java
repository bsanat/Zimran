package kz.astyq.zimran.subscription.view.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.subscription.model.dto.SubscriptionViewResponse;

public interface SubscriptionViewService {

    SubscriptionViewResponse getSubscription(RequestScope scope);
}
