package kz.astyq.zimran.assistant.service;

import kz.astyq.zimran.assistant.model.dto.AssistantRecommendRequest;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;

/**
 * Business validation of an assistant recommendation request. Strict: the guest confirmed these filters, so a
 * broken rule fails the request.
 */
public interface AssistantRequestValidationService {

    /**
     * The same rules as the planner and as {@code POST /api/ai/parse}: supported city, at least one known category
     * slug (unknown ones are dropped), {@code dateFrom} today or later in the city, {@code dateTo} (default
     * {@code dateFrom}) not before it and within {@code app.planner.max-range-days}, budget {@code >= 0}, notes at
     * most 500 characters; {@code timeOfDay} defaults to {@code any}. The first broken rule:
     * {@code ServiceValidationException(VALIDATION_ERROR)} with {@code details.field} named as in the request
     * ({@code dateFrom}, {@code dateTo}, ...).
     */
    ValidatedPlanRequest validate(AssistantRecommendRequest request);
}
