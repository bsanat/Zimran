package kz.astyq.zimran.health.view.service;

import kz.astyq.zimran.health.model.dto.HealthViewResponse;

/**
 * Liveness of the API process.
 */
public interface HealthViewService {

    /**
     * Always {@code {"ok": true}} while the process serves requests; it does not check the event source or AI.
     */
    HealthViewResponse getHealth();
}
