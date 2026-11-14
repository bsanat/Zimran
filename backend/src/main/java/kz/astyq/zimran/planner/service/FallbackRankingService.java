package kz.astyq.zimran.planner.service;

import kz.astyq.zimran.planner.model.domain.CandidateSet;
import kz.astyq.zimran.planner.model.domain.Recommendation;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;

import java.util.List;

/**
 * Rule-based ranking used when the AI is not available: the best candidates by pre-score.
 */
public interface FallbackRankingService {

    /**
     * The first {@code app.planner.max-recommendations} candidates (already best first), each with its pre-score
     * as match score and a deterministic reason.
     */
    List<Recommendation> rank(CandidateSet candidateSet, ValidatedPlanRequest request);
}
