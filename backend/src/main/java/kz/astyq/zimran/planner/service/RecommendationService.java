package kz.astyq.zimran.planner.service;

import kz.astyq.zimran.planner.model.domain.CandidateSet;
import kz.astyq.zimran.planner.model.domain.Plan;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;

import java.util.Optional;

/**
 * Picks and explains the plan's events from the candidates. The one ranking pipeline of the planner
 * ({@code POST /api/plan}) and the AI assistant ({@code POST /api/ai/recommend}).
 */
public interface RecommendationService {

    /**
     * Like {@link #recommendByAi}, with model-written suggestions; when the AI is unavailable, fails, answers
     * off-schema or every pick is invalid, the rule-based ranking answers instead.
     */
    Plan recommend(ValidatedPlanRequest request, CandidateSet candidateSet, String language);

    /**
     * Asks the AI to rank the candidates (texts in {@code language}); keeps only picks that are candidates,
     * deduplicated, at most {@code app.planner.max-recommendations}, scores clamped to 0..100. The model writes
     * only the summary and the reasons; suggestions are the candidate set's hints. A valid answer with no picks is
     * kept (nothing fits). Empty when there are no candidates, the AI is unavailable, fails, answers off-schema or
     * named only events that are not candidates; never a rule-based result.
     */
    Optional<Plan> recommendByAi(ValidatedPlanRequest request, CandidateSet candidateSet, String language);
}
