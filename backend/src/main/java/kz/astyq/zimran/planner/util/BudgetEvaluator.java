package kz.astyq.zimran.planner.util;

import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.planner.model.enums.BudgetFit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Compares an event's price with the guest's budget (USD). An unknown price is never treated as free; a price
 * in another currency, or without a currency, counts as unknown because it cannot be compared.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BudgetEvaluator {

    static final String BUDGET_CURRENCY = "USD";

    /**
     * <ul>
     *   <li>no budget: WITHIN if the price is known, else UNKNOWN (never excluded);</li>
     *   <li>a budget (0 = free only): WITHIN only for a known USD minimum price at or below it; everything else,
     *       an unknown price included, is OVER.</li>
     * </ul>
     * OVER means "exclude from the plan".
     */
    public static BudgetFit evaluate(Event event, BigDecimal budgetMax) {
        Objects.requireNonNull(event, "event");
        boolean comparable = isComparable(event);
        if (budgetMax == null) {
            return comparable ? BudgetFit.WITHIN : BudgetFit.UNKNOWN;
        }
        return comparable && event.getPriceMin().compareTo(budgetMax) <= 0 ? BudgetFit.WITHIN : BudgetFit.OVER;
    }

    private static boolean isComparable(Event event) {
        return event.isPriceKnown()
                && event.getPriceMin() != null
                && BUDGET_CURRENCY.equalsIgnoreCase(event.getCurrency());
    }
}
