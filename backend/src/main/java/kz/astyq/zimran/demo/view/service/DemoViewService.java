package kz.astyq.zimran.demo.view.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.subscription.model.dto.SubscriptionViewResponse;

public interface DemoViewService {

    /**
     * "Try Premium Demo": gives the caller demo Premium and returns the updated subscription.
     */
    SubscriptionViewResponse activatePremium(RequestScope scope);
}
