package kz.astyq.zimran.assistant.service.impl;

import kz.astyq.zimran.assistant.model.dto.AssistantRecommendRequest;
import kz.astyq.zimran.assistant.service.AssistantRequestValidationService;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.model.dto.PlanCreateRequest;
import kz.astyq.zimran.planner.service.PlanRequestValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Delegates to the planner's validation, so a recommendation is checked exactly like {@code POST /api/plan}. Each
 * category slug stands for all of its interests.
 */
@Service
@RequiredArgsConstructor
public class AssistantRequestValidationServiceImpl implements AssistantRequestValidationService {

    /** Planner field name -> assistant request field name. */
    private static final Map<String, String> FIELD_NAMES = Map.of(
            "startDate", "dateFrom",
            "endDate", "dateTo",
            "timePreference", "timeOfDay");

    private final PlanRequestValidationService planRequestValidationService;

    @Override
    public ValidatedPlanRequest validate(AssistantRecommendRequest request) {
        Objects.requireNonNull(request, "request");
        PlanCreateRequest planRequest = PlanCreateRequest.builder()
                .city(request.getCity())
                .interests(interests(request.getInterests()))
                .startDate(request.getDateFrom())
                .endDate(request.getDateTo())
                .budgetMax(request.getBudgetMax())
                .timePreference(request.getTimeOfDay())
                .notes(request.getNotes())
                .build();
        ValidatedPlanRequest validated;
        try {
            validated = planRequestValidationService.validate(planRequest);
        } catch (ServiceValidationException e) {
            Object field = e.getDetails().get("field");
            String renamed = field != null ? FIELD_NAMES.get(field.toString()) : null;
            throw renamed != null ? e.withDetail("field", renamed) : e;
        }
        String message = request.getMessage();
        return validated.toBuilder().message(StringUtils.hasText(message) ? message.strip() : null).build();
    }

    private static List<Interest> interests(List<String> slugIds) {
        if (slugIds == null) {
            return List.of();
        }
        return slugIds.stream()
                .map(CategorySlug::fromId)
                .flatMap(Optional::stream)
                .distinct()
                .flatMap(slug -> slug.interests().stream())
                .toList();
    }
}
