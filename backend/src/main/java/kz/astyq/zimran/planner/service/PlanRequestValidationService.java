package kz.astyq.zimran.planner.service;

import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.model.dto.PlanCreateRequest;

/**
 * Business validation of a plan request.
 */
public interface PlanRequestValidationService {

    /**
     * Checks every rule (supported city, at least one interest, start date today or later in the city,
     * end date not before it and within {@code app.planner.max-range-days}, budget >= 0, notes <= 500 characters), applies defaults ({@code endDate = startDate}, {@code timePreference = ANY}) and
     * sanitizes {@code notes}. The first broken rule is thrown as
     * {@code ServiceValidationException(VALIDATION_ERROR, ...)} with detail {@code field=<name>}.
     */
    ValidatedPlanRequest validate(PlanCreateRequest request);
}
