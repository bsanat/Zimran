package kz.astyq.zimran.planner.service;

import kz.astyq.zimran.planner.model.domain.CandidateSet;
import kz.astyq.zimran.planner.model.domain.Plan;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;

/**
 * Picks and explains the plan's events from the candidates.
 */
public interface RecommendationService {

    /**
     * Asks the AI to rank the candidates (texts in {@code language}); keeps only picks that are candidates, at most
     * {@code app.planner.max-recommendations}. When the AI is unavailable, fails, answers off-schema or every pick
     * is invalid, the rule-based ranking answers instead. A valid AI answer with no picks is kept (nothing fits).
     */
    Plan recommend(ValidatedPlanRequest request, CandidateSet candidateSet, String language);
}
