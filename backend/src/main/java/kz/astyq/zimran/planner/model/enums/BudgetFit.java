package kz.astyq.zimran.planner.model.enums;

/**
 * How an event's price relates to the requested budget. {@link #OVER} events are excluded from the plan.
 */
public enum BudgetFit {
    WITHIN,
    UNKNOWN,
    OVER
}
