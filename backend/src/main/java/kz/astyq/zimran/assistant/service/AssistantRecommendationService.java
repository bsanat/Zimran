package kz.astyq.zimran.assistant.service;

import kz.astyq.zimran.assistant.model.domain.AssistantRecommendation;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;

/**
 * AI-ranked recommendations for the assistant, on the planner's candidate and ranking pipeline.
 */
public interface AssistantRecommendationService {

    /**
     * <ol>
     *   <li>Checks the scope's {@code assistant} quota (free tier / Premium fair use) without counting it.</li>
     *   <li>Filters the catalog in code ({@code CandidateService}: city, dates, interests, time of day, budget with
     *       unknown prices excluded, scheduled events only) and keeps the best {@code app.ai.max-candidates}.</li>
     *   <li>No candidates: no AI call and no quota use; empty recommendations with relaxation suggestions.</li>
     *   <li>Otherwise the AI ranks ({@code RecommendationService#recommendByAi}); then one use is counted.
     *       AI unavailable: {@code ServiceValidationException(AI_UNAVAILABLE)}, nothing counted, never a
     *       rule-based or guessed result.</li>
     * </ol>
     * Quota exhausted: {@code 403 FREE_LIMIT_REACHED} or {@code 429 RATE_LIMITED} (Premium fair use).
     */
    AssistantRecommendation recommend(ValidatedPlanRequest request, RequestScope scope);
}
