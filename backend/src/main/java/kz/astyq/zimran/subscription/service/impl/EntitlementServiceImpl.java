package kz.astyq.zimran.subscription.service.impl;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.subscription.model.domain.Entitlement;
import kz.astyq.zimran.subscription.model.entity.SubscriptionEntitlement;
import kz.astyq.zimran.subscription.model.enums.SubscriptionStatus;
import kz.astyq.zimran.subscription.repository.SubscriptionEntitlementRepository;
import kz.astyq.zimran.subscription.service.DemoEntitlementService;
import kz.astyq.zimran.subscription.service.EntitlementService;
import kz.astyq.zimran.subscription.util.MessageCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class EntitlementServiceImpl implements EntitlementService, DemoEntitlementService {

    private final SubscriptionEntitlementRepository repository;
    private final Clock clock;

    @Override
    public Entitlement getEntitlement(RequestScope scope) {
        return repository.findByScopeId(scope.getScopeId())
                .map(EntitlementServiceImpl::toDomain)
                .orElse(Entitlement.FREE);
    }

    @Override
    public SubscriptionStatus getStatus(RequestScope scope) {
        return getEntitlement(scope).getStatus();
    }

    @Override
    public boolean isPremium(RequestScope scope) {
        return getStatus(scope).isPremium();
    }

    @Override
    public void requirePremium(RequestScope scope) {
        if (!isPremium(scope)) {
            throw new ServiceValidationException(ErrorCode.PREMIUM_REQUIRED, MessageCode.PREMIUM_REQUIRED);
        }
    }

    @Override
    @Transactional
    public void activateDemoPremium(RequestScope scope) {
        repository.upsertDemoPremium(scope.getScopeId(), clock.instant());
    }

    @Override
    @Transactional
    public void deactivateDemoPremium(RequestScope scope) {
        repository.downgradeDemoPremium(scope.getScopeId(), clock.instant());
    }

    private static Entitlement toDomain(SubscriptionEntitlement entity) {
        return Entitlement.builder()
                .status(entity.getStatus())
                .demoOnly(entity.isDemoOnly())
                .periodEnd(entity.getPeriodEnd())
                .build();
    }
}
