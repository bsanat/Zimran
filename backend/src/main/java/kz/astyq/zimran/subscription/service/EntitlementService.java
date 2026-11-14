package kz.astyq.zimran.subscription.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.subscription.model.domain.Entitlement;
import kz.astyq.zimran.subscription.model.enums.SubscriptionStatus;

/**
 * The single place that decides whether a scope is Premium. Every Premium check goes through here.
 */
public interface EntitlementService {

    Entitlement getEntitlement(RequestScope scope);

    SubscriptionStatus getStatus(RequestScope scope);

    /**
     * {@code DEMO_PREMIUM} or {@code PREMIUM_ACTIVE}.
     */
    boolean isPremium(RequestScope scope);

    /**
     * Throws {@code ServiceValidationException(PREMIUM_REQUIRED)} (403) unless the scope is Premium.
     */
    void requirePremium(RequestScope scope);
}
