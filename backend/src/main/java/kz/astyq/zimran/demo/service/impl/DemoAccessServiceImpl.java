package kz.astyq.zimran.demo.service.impl;

import kz.astyq.zimran.core.configuration.DemoProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.demo.service.DemoAccessService;
import kz.astyq.zimran.demo.util.MessageCode;
import kz.astyq.zimran.subscription.model.enums.SubscriptionStatus;
import kz.astyq.zimran.subscription.service.EntitlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DemoAccessServiceImpl implements DemoAccessService {

    private final DemoProperties demoProperties;
    private final EntitlementService entitlementService;

    @Override
    public void requireEnabled() {
        if (!demoProperties.isEnabled()) {
            throw new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.DEMO_DISABLED);
        }
    }

    @Override
    public void requireDemoPremium(RequestScope scope) {
        requireEnabled();
        if (entitlementService.getStatus(scope) != SubscriptionStatus.DEMO_PREMIUM) {
            throw new ServiceValidationException(ErrorCode.DEMO_PREMIUM_REQUIRED, MessageCode.DEMO_PREMIUM_REQUIRED);
        }
    }
}
