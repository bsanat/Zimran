package kz.astyq.zimran.subscription.service.impl;

import kz.astyq.zimran.core.configuration.SubscriptionProperties;
import kz.astyq.zimran.core.i18n.MessageService;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.subscription.mapper.SubscriptionMapper;
import kz.astyq.zimran.subscription.model.dto.SubscriptionLimitsViewResponse;
import kz.astyq.zimran.subscription.model.dto.SubscriptionViewResponse;
import kz.astyq.zimran.subscription.model.enums.UsageFeature;
import kz.astyq.zimran.subscription.service.EntitlementService;
import kz.astyq.zimran.subscription.service.SubscriptionSummaryService;
import kz.astyq.zimran.subscription.service.UsageQuotaService;
import kz.astyq.zimran.subscription.util.MessageCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionSummaryServiceImpl implements SubscriptionSummaryService {

    private final EntitlementService entitlementService;
    private final UsageQuotaService usageQuotaService;
    private final SubscriptionProperties properties;
    private final SubscriptionMapper subscriptionMapper;
    private final MessageService messageService;

    @Override
    public SubscriptionViewResponse getSummary(RequestScope scope) {
        SubscriptionProperties.Plan plan = properties.getPlan();
        List<String> featureLabels = plan.getFeatures().stream()
                .map(id -> messageService.getMessage(MessageCode.planFeature(id)))
                .toList();
        SubscriptionLimitsViewResponse limits = SubscriptionLimitsViewResponse.builder()
                .assistantSessionsLeft(usageQuotaService.remaining(scope, UsageFeature.ASSISTANT))
                .pairSessionsLeft(usageQuotaService.remaining(scope, UsageFeature.PAIR))
                .build();
        return subscriptionMapper.toViewResponse(entitlementService.getEntitlement(scope),
                subscriptionMapper.toPlanViewResponse(plan, featureLabels), limits);
    }
}
