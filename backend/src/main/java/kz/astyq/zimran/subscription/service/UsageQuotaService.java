package kz.astyq.zimran.subscription.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.subscription.model.enums.UsageFeature;

/**
 * Usage quotas per scope and feature: the free tier, and a fair-use limit for Premium
 * ({@code app.subscription.quotas}).
 */
public interface UsageQuotaService {

    /**
     * Counts one use. Over the free quota: {@code 403 FREE_LIMIT_REACHED}; over the Premium fair-use limit:
     * {@code 429 RATE_LIMITED} with {@code Retry-After}. Both carry {@code details.feature}.
     */
    void consume(RequestScope scope, UsageFeature feature);

    /**
     * Checks without counting that one more use is allowed, with the same errors as {@link #consume}. For costly
     * work that should only be counted once it succeeded: check first, {@link #consume} after.
     */
    void requireAvailable(RequestScope scope, UsageFeature feature);

    /**
     * Uses left in the current window for the scope's tier.
     */
    int remaining(RequestScope scope, UsageFeature feature);
}
