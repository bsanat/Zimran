package kz.astyq.zimran.demo.view.service.impl;

import kz.astyq.zimran.core.configuration.DemoProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.demo.util.MessageCode;
import kz.astyq.zimran.demo.view.service.DemoViewService;
import kz.astyq.zimran.subscription.model.dto.SubscriptionViewResponse;
import kz.astyq.zimran.subscription.service.DemoEntitlementService;
import kz.astyq.zimran.subscription.service.SubscriptionSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DemoViewServiceImpl implements DemoViewService {

    private final DemoProperties demoProperties;
    private final DemoEntitlementService demoEntitlementService;
    private final SubscriptionSummaryService subscriptionSummaryService;

    @Override
    public SubscriptionViewResponse activatePremium(RequestScope scope) {
        if (!demoProperties.isEnabled()) {
            throw new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.DEMO_DISABLED);
        }
        demoEntitlementService.activateDemoPremium(scope);
        return subscriptionSummaryService.getSummary(scope);
    }
}
