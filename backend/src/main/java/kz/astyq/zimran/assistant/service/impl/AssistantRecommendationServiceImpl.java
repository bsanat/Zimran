package kz.astyq.zimran.assistant.service.impl;

import kz.astyq.zimran.ai.util.MessageCode;
import kz.astyq.zimran.assistant.model.domain.AssistantRecommendation;
import kz.astyq.zimran.assistant.service.AssistantRecommendationService;
import kz.astyq.zimran.assistant.util.RelaxationSuggestions;
import kz.astyq.zimran.core.configuration.AiAssistantProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.model.AppSettings;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.planner.model.domain.CandidateSet;
import kz.astyq.zimran.planner.model.domain.Plan;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.service.CandidateService;
import kz.astyq.zimran.planner.service.PlanExplanationService;
import kz.astyq.zimran.planner.service.RecommendationService;
import kz.astyq.zimran.subscription.model.enums.UsageFeature;
import kz.astyq.zimran.subscription.service.UsageQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantRecommendationServiceImpl implements AssistantRecommendationService {

    private final UsageQuotaService usageQuotaService;
    private final CandidateService candidateService;
    private final RecommendationService recommendationService;
    private final PlanExplanationService planExplanationService;
    private final AiAssistantProperties aiAssistantProperties;
    private final AppSettings appSettings;

    @Override
    public AssistantRecommendation recommend(ValidatedPlanRequest request, RequestScope scope) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(scope, "scope");
        usageQuotaService.requireAvailable(scope, UsageFeature.ASSISTANT);

        CandidateSet candidateSet = limit(candidateService.buildCandidates(request));
        if (candidateSet.getCandidates().isEmpty()) {
            return AssistantRecommendation.builder()
                    .summary(planExplanationService.summaryFor(0, request))
                    .recommendations(List.of())
                    .relaxations(RelaxationSuggestions.forRequest(request))
                    .build();
        }

        Plan plan = recommendationService.recommendByAi(request, candidateSet, appSettings.getLocale())
                .orElseThrow(() -> new ServiceValidationException(ErrorCode.AI_UNAVAILABLE, MessageCode.AI_UNAVAILABLE));
        usageQuotaService.consume(scope, UsageFeature.ASSISTANT);
        log.info("Assistant plan {}: {} candidates, {} recommendations", plan.getId(),
                candidateSet.getCandidates().size(), plan.getRecommendations().size());
        return AssistantRecommendation.builder()
                .summary(plan.getSummary())
                .recommendations(plan.getRecommendations())
                .relaxations(plan.getRecommendations().isEmpty() ? RelaxationSuggestions.forRequest(request) : List.of())
                .build();
    }

    /**
     * The best {@code app.ai.max-candidates} candidates (the set is already best first).
     */
    private CandidateSet limit(CandidateSet candidateSet) {
        int max = aiAssistantProperties.getMaxCandidates();
        if (candidateSet.getCandidates().size() <= max) {
            return candidateSet;
        }
        return CandidateSet.builder()
                .candidates(List.copyOf(candidateSet.getCandidates().subList(0, max)))
                .stats(candidateSet.getStats())
                .hints(candidateSet.getHints())
                .build();
    }
}
