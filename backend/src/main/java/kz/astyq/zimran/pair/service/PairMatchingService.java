package kz.astyq.zimran.pair.service;

import kz.astyq.zimran.pair.model.domain.PairMatch;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;

/**
 * Deterministic matching of two sides (the AI never decides which events fit both).
 */
public interface PairMatchingService {

    /**
     * <ol>
     *   <li>Intersects the dates and times of day; no overlap: {@code NO_MATCH} with conflict {@code DATES}.</li>
     *   <li>Strict budget: the lower of the budgets given (one given: that one; none: no limit).</li>
     *   <li>Filters the city's ACTIVE catalog events by the intersection, the budget and the union of interests
     *   (planner rules: an unknown price never passes a budget).</li>
     *   <li>Events in an interest of both sides come first; events matching one side's interest are compromises
     *   (alternating between the sides), used only to reach {@code app.pair.min-suggestions}. At most
     *   {@code app.pair.max-suggestions}.</li>
     *   <li>No event: {@code NO_MATCH} with the constraint that removed the most events.</li>
     * </ol>
     * Both requests must be for the same city.
     */
    PairMatch match(ValidatedPlanRequest a, ValidatedPlanRequest b);
}
