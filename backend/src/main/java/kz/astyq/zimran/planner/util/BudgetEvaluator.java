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
     *   <li>budget 0 (free only): WITHIN only for a known minimum price of 0, everything else OVER;</li>
     *   <li>budget above 0: WITHIN if the known minimum price fits, else OVER; an unknown price is UNKNOWN,
     *       or OVER when {@code includeUnknownPrice} is false.</li>
     * </ul>
     * OVER means "exclude from the plan".
     */
    public static BudgetFit evaluate(Event event, BigDecimal budgetMax, boolean includeUnknownPrice) {
        Objects.requireNonNull(event, "event");
        boolean comparable = isComparable(event);
        if (budgetMax == null) {
            return comparable ? BudgetFit.WITHIN : BudgetFit.UNKNOWN;
        }
        if (budgetMax.signum() == 0) {
            return comparable && event.getPriceMin().signum() == 0 ? BudgetFit.WITHIN : BudgetFit.OVER;
        }
        if (!comparable) {
            return includeUnknownPrice ? BudgetFit.UNKNOWN : BudgetFit.OVER;
        }
        return event.getPriceMin().compareTo(budgetMax) <= 0 ? BudgetFit.WITHIN : BudgetFit.OVER;
    }

    private static boolean isComparable(Event event) {
        return event.isPriceKnown()
                && event.getPriceMin() != null
                && BUDGET_CURRENCY.equalsIgnoreCase(event.getCurrency());
    }
}
