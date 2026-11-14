package kz.astyq.zimran.planner.service.impl;

import kz.astyq.zimran.ai.model.domain.AiRanking;
import kz.astyq.zimran.ai.service.AiRankingService;
import kz.astyq.zimran.core.configuration.PlannerProperties;
import kz.astyq.zimran.planner.model.domain.Candidate;
import kz.astyq.zimran.planner.model.domain.CandidateSet;
import kz.astyq.zimran.planner.model.domain.Plan;
import kz.astyq.zimran.planner.model.domain.Recommendation;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.service.FallbackRankingService;
import kz.astyq.zimran.planner.service.PlanExplanationService;
import kz.astyq.zimran.planner.service.RecommendationService;
import kz.astyq.zimran.planner.util.AiPickFilter;
import kz.astyq.zimran.planner.util.AiRankingRequestFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    static final int MAX_SUGGESTIONS = 3;

    private final AiRankingService aiRankingService;
    private final FallbackRankingService fallbackRankingService;
    private final PlanExplanationService planExplanationService;
    private final PlannerProperties plannerProperties;

    @Override
    public Plan recommend(ValidatedPlanRequest request, CandidateSet candidateSet, String language) {
        Optional<Plan> aiPlan = aiPlan(request, candidateSet, language, true);
        if (aiPlan.isPresent()) {
            return aiPlan.get();
        }

        List<Recommendation> recommendations = fallbackRankingService.rank(candidateSet, request);
        List<String> suggestions = new ArrayList<>(candidateSet.getHints());
        if (request.getNotes() != null && !recommendations.isEmpty()) {
            suggestions.add(planExplanationService.notesNotUsed());
        }
        return Plan.builder()
                .id(UUID.randomUUID())
                .summary(planExplanationService.summaryFor(recommendations.size(), request))
                .recommendations(recommendations)
                .suggestions(limit(suggestions))
                .aiRanked(false)
                .build();
    }

    @Override
    public Optional<Plan> recommendByAi(ValidatedPlanRequest request, CandidateSet candidateSet, String language) {
        return aiPlan(request, candidateSet, language, false);
    }

    private Optional<Plan> aiPlan(ValidatedPlanRequest request, CandidateSet candidateSet, String language,
                                  boolean withSuggestions) {
        List<Candidate> candidates = candidateSet.getCandidates();
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        Optional<AiRanking> ranking = aiRankingService.rank(AiRankingRequestFactory.create(request, candidates,
                language, plannerProperties.getMaxRecommendations(), withSuggestions));
        if (ranking.isEmpty()) {
            return Optional.empty();
        }
        Optional<Plan> plan = fromAi(ranking.get(), request, candidateSet);
        if (plan.isEmpty()) {
            log.warn("AI picked no valid candidate");
        }
        return plan;
    }

    /**
     * The AI's plan, or empty when it named events but none of them is a candidate.
     */
    private Optional<Plan> fromAi(AiRanking ranking, ValidatedPlanRequest request, CandidateSet candidateSet) {
        Map<String, Candidate> byId = new LinkedHashMap<>();
        candidateSet.getCandidates().forEach(candidate -> byId.put(candidate.getEvent().getId().asString(), candidate));
        List<Recommendation> recommendations = AiPickFilter.filter(ranking.getPicks(), byId,
                plannerProperties.getMaxRecommendations(),
                candidate -> planExplanationService.reasonFor(candidate, request));
        if (recommendations.isEmpty() && !ranking.getPicks().isEmpty()) {
            return Optional.empty();
        }
        String summary = ranking.getSummary().isBlank()
                ? planExplanationService.summaryFor(recommendations.size(), request) : ranking.getSummary();
        List<String> suggestions = !ranking.getSuggestions().isEmpty() ? ranking.getSuggestions() : candidateSet.getHints();
        return Optional.of(Plan.builder()
                .id(UUID.randomUUID())
                .summary(summary)
                .recommendations(recommendations)
                .suggestions(limit(suggestions))
                .aiRanked(true)
                .build());
    }

    private static List<String> limit(List<String> suggestions) {
        return suggestions.stream().limit(MAX_SUGGESTIONS).toList();
    }
}
