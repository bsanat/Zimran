package kz.astyq.zimran.subscription.service.impl;

import kz.astyq.zimran.core.configuration.SubscriptionProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.subscription.model.enums.UsageFeature;
import kz.astyq.zimran.subscription.repository.UsageCounterRepository;
import kz.astyq.zimran.subscription.service.EntitlementService;
import kz.astyq.zimran.subscription.service.UsageQuotaService;
import kz.astyq.zimran.subscription.util.MessageCode;
import kz.astyq.zimran.subscription.util.QuotaWindows;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Database-backed quotas ({@code usage_counters}): one row per scope, feature and window, incremented atomically
 * so concurrent requests cannot exceed the limit.
 */
@Service
@RequiredArgsConstructor
public class UsageQuotaServiceImpl implements UsageQuotaService {

    private final UsageCounterRepository repository;
    private final EntitlementService entitlementService;
    private final SubscriptionProperties properties;
    private final Clock clock;

    @Override
    @Transactional
    public void consume(RequestScope scope, UsageFeature feature) {
        boolean premium = entitlementService.isPremium(scope);
        SubscriptionProperties.Tier tier = tier(feature, premium);
        Instant now = clock.instant();
        Instant windowStart = QuotaWindows.start(now, tier.getWindow());
        boolean counted = tier.getLimit() > 0 && repository.incrementIfBelow(
                scope.getScopeId(), feature.name(), windowStart, tier.getLimit(), now).isPresent();
        if (!counted) {
            throw limitReached(feature, premium, tier, now);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void requireAvailable(RequestScope scope, UsageFeature feature) {
        boolean premium = entitlementService.isPremium(scope);
        SubscriptionProperties.Tier tier = tier(feature, premium);
        Instant now = clock.instant();
        if (remaining(scope, feature, tier, now) <= 0) {
            throw limitReached(feature, premium, tier, now);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int remaining(RequestScope scope, UsageFeature feature) {
        SubscriptionProperties.Tier tier = tier(feature, entitlementService.isPremium(scope));
        return remaining(scope, feature, tier, clock.instant());
    }

    private int remaining(RequestScope scope, UsageFeature feature, SubscriptionProperties.Tier tier, Instant now) {
        Instant windowStart = QuotaWindows.start(now, tier.getWindow());
        int used = repository.findUsageCount(scope.getScopeId(), feature, windowStart).orElse(0);
        return Math.max(0, tier.getLimit() - used);
    }

    private SubscriptionProperties.Tier tier(UsageFeature feature, boolean premium) {
        SubscriptionProperties.Quota quota = properties.quotaFor(feature);
        return premium ? quota.getPremium() : quota.getFree();
    }

    private static ServiceValidationException limitReached(UsageFeature feature, boolean premium,
                                                           SubscriptionProperties.Tier tier, Instant now) {
        Instant resetsAt = QuotaWindows.end(now, tier.getWindow());
        ServiceValidationException error = premium
                ? new ServiceValidationException(ErrorCode.RATE_LIMITED, MessageCode.FAIR_USE_LIMIT_REACHED)
                : new ServiceValidationException(ErrorCode.FREE_LIMIT_REACHED, MessageCode.FREE_LIMIT_REACHED);
        error.withDetail("feature", feature.getId())
                .withDetail("limit", tier.getLimit())
                .withDetail("resetsAt", resetsAt != null ? resetsAt.toString() : null);
        if (premium && resetsAt != null) {
            error.withRetryAfter(Math.max(1, Duration.between(now, resetsAt).toSeconds()));
        }
        return error;
    }
}
