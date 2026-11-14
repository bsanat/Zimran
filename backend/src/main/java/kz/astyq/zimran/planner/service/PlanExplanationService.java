package kz.astyq.zimran.planner.service;

import kz.astyq.zimran.planner.model.domain.Candidate;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;

/**
 * Deterministic plan texts in the request's language, built only from facts the planner checked.
 */
public interface PlanExplanationService {

    /**
     * Why {@code candidate} fits: its matched interests, plus the time of day and budget when the guest asked for
     * them and the event satisfies them.
     */
    String reasonFor(Candidate candidate, ValidatedPlanRequest request);

    /**
     * One sentence about a plan with {@code count} recommendations.
     */
    String summaryFor(int count, ValidatedPlanRequest request);

    /**
     * Tells the guest their notes were not used (rule-based ranking cannot read them).
     */
    String notesNotUsed();
}
