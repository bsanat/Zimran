package kz.astyq.zimran.planner.service;

import kz.astyq.zimran.planner.model.domain.CandidateSet;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;

/**
 * Turns a validated plan request into the set of real events the AI may choose from.
 */
public interface CandidateService {

    /**
     * Fetches the city's dated events for the range and removes, in this order (each counted in the stats):
     * events not scheduled (cancelled/postponed), already started, outside the date range, matching no requested
     * interest, at the wrong time of day (or without a known time when a time is requested), over budget (with a
     * budget, an unknown price never passes). The rest are scored, sorted by score (desc), start and
     * title, limited to 3 per venue per day and to {@code app.planner.max-candidates}. Hints are added when fewer
     * than 3 candidates remain.
     */
    CandidateSet buildCandidates(ValidatedPlanRequest request);
}
