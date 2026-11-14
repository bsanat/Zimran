package kz.astyq.zimran.demo.service;

import kz.astyq.zimran.core.model.domain.RequestScope;

/**
 * Who may use demo-only features.
 */
public interface DemoAccessService {

    /**
     * {@code 404 NOT_FOUND} unless {@code app.demo.enabled=true}.
     */
    void requireEnabled();

    /**
     * {@link #requireEnabled()}, then {@code 403 DEMO_PREMIUM_REQUIRED} unless the scope is {@code DEMO_PREMIUM}.
     */
    void requireDemoPremium(RequestScope scope);
}
