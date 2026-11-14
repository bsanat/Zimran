package kz.astyq.zimran.planner.view.service;

import kz.astyq.zimran.planner.model.dto.PlanCreateRequest;
import kz.astyq.zimran.planner.model.dto.PlanViewResponse;

/**
 * Guest plan creation.
 */
public interface PlannerViewService {

    /**
     * Validates the request, collects the candidate events and ranks them (AI, else rule-based) in the request's
     * language. Invalid request: {@code VALIDATION_ERROR} with a {@code field} detail; event source down:
     * {@code UPSTREAM_UNAVAILABLE}.
     */
    PlanViewResponse createPlan(PlanCreateRequest request);
}
