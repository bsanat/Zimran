package kz.astyq.zimran.pair.service.impl;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.pair.model.dto.PairPreferencesRequest;
import kz.astyq.zimran.pair.service.PairPreferencesValidationService;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.model.dto.PlanCreateRequest;
import kz.astyq.zimran.planner.service.PlanRequestValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * Delegates to the planner's validation, so both sides are checked exactly like a plan request.
 */
@Service
@RequiredArgsConstructor
public class PairPreferencesValidationServiceImpl implements PairPreferencesValidationService {

    private static final String CITY_FIELD = "city";
    /** Planner field name -> pair request field name. */
    private static final Map<String, String> FIELD_NAMES = Map.of(
            "startDate", "dateFrom",
            "endDate", "dateTo",
            "notes", "message");

    private final PlanRequestValidationService planRequestValidationService;

    @Override
    public ValidatedPlanRequest validate(PairPreferencesRequest preferences, String city, String fieldPrefix) {
        Objects.requireNonNull(preferences, "preferences");
        PlanCreateRequest planRequest = PlanCreateRequest.builder()
                .city(city)
                .interests(preferences.getInterests())
                .startDate(preferences.getDateFrom())
                .endDate(preferences.getDateTo())
                .budgetMax(preferences.getBudgetMax())
                .timePreference(preferences.getTimePreference())
                .notes(preferences.getMessage())
                .build();
        try {
            return planRequestValidationService.validate(planRequest);
        } catch (ServiceValidationException e) {
            Object field = e.getDetails().get("field");
            if (field == null || CITY_FIELD.equals(field.toString())) {
                throw e;
            }
            String name = FIELD_NAMES.getOrDefault(field.toString(), field.toString());
            throw e.withDetail("field", fieldPrefix + "." + name);
        }
    }
}
