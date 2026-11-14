package kz.astyq.zimran.subscription.view.service.impl;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.subscription.model.dto.SubscriptionViewResponse;
import kz.astyq.zimran.subscription.service.SubscriptionSummaryService;
import kz.astyq.zimran.subscription.view.service.SubscriptionViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionViewServiceImpl implements SubscriptionViewService {

    private final SubscriptionSummaryService subscriptionSummaryService;

    @Override
    public SubscriptionViewResponse getSubscription(RequestScope scope) {
        return subscriptionSummaryService.getSummary(scope);
    }
}
