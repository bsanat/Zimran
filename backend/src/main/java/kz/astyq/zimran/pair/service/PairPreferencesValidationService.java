package kz.astyq.zimran.pair.service;

import kz.astyq.zimran.pair.model.dto.PairPreferencesRequest;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;

/**
 * Business validation of one side's wishes, with exactly the rules of {@code POST /api/plan}.
 */
public interface PairPreferencesValidationService {

    /**
     * Validates {@code preferences} for {@code city} (id or name). The first broken rule is thrown as
     * {@code ServiceValidationException(VALIDATION_ERROR, ...)} with {@code details.field} =
     * {@code <fieldPrefix>.<field>} ({@code city} for the city). The message is sanitized and kept as
     * {@code notes}.
     */
    ValidatedPlanRequest validate(PairPreferencesRequest preferences, String city, String fieldPrefix);
}
