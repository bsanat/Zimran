package kz.astyq.zimran.pair.service;

import kz.astyq.zimran.pair.model.enums.PairConflictType;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;

/**
 * Explains a missing match: which constraint removed the most events, and which one to relax.
 */
public interface PairConflictService {

    /**
     * For a combined request without candidates: the constraint whose removal brings back the most events —
     * {@code BUDGET} (no budget), {@code DATES} (the union of both sides' dates, any time of day) or
     * {@code INTERESTS} (every interest). Ties go in that order; when nothing comes back: {@code DATES}.
     */
    PairConflictType mostRestrictive(ValidatedPlanRequest combined, ValidatedPlanRequest a, ValidatedPlanRequest b);

    /**
     * The one constraint to suggest relaxing after the votes found no common event: the strict budget when
     * there is one, else the interests when the sides share none, else the dates.
     */
    PairConflictType relaxationAfterVotes(ValidatedPlanRequest a, ValidatedPlanRequest b);
}
