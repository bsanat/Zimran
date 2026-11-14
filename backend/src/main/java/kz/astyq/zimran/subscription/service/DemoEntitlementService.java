package kz.astyq.zimran.subscription.service;

import kz.astyq.zimran.core.model.domain.RequestScope;

/**
 * Grants and ends demo Premium. Callers must check that demo features are enabled.
 */
public interface DemoEntitlementService {

    /**
     * Sets {@code DEMO_PREMIUM} (demo only) for the scope. Idempotent; never downgrades a paid subscription.
     */
    void activateDemoPremium(RequestScope scope);

    /**
     * Returns a {@code DEMO_PREMIUM} scope to {@code FREE}. Idempotent; never touches a paid subscription.
     */
    void deactivateDemoPremium(RequestScope scope);
}
