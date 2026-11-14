package kz.astyq.zimran.planner.util;

import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.TestEvents;
import kz.astyq.zimran.planner.model.enums.BudgetFit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetEvaluatorTest {

    /**
     * Price kinds: FREE (known, USD 0), KNOWN_20 / KNOWN_40 (known, USD), UNKNOWN, EUR (known 10, in EUR).
     */
    @ParameterizedTest(name = "budget {0}, price {1} -> {2}")
    @DisplayName("evaluate: budget x price matrix")
    @CsvSource(nullValues = "NULL", value = {
            "NULL, FREE,     WITHIN",
            "NULL, KNOWN_20, WITHIN",
            "NULL, KNOWN_40, WITHIN",
            "NULL, UNKNOWN,  UNKNOWN",
            "NULL, EUR,      UNKNOWN",
            "0,    FREE,     WITHIN",
            "0,    KNOWN_20, OVER",
            "0,    UNKNOWN,  OVER",
            "0,    EUR,      OVER",
            "25,   FREE,     WITHIN",
            "25,   KNOWN_20, WITHIN",
            "25,   KNOWN_40, OVER",
            "25,   UNKNOWN,  OVER",
            "25,   EUR,      OVER"
    })
    void matrix(BigDecimal budget, String price, BudgetFit expected) {
        assertThat(BudgetEvaluator.evaluate(event(price), budget)).isEqualTo(expected);
    }

    @Nested
    @DisplayName("evaluate: edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a minimum price equal to the budget fits")
        void priceEqualToBudget() {
            assertThat(BudgetEvaluator.evaluate(priced(new BigDecimal("25.00"), "USD"), new BigDecimal("25")))
                    .isEqualTo(BudgetFit.WITHIN);
        }

        @Test
        @DisplayName("budget 0.00 behaves like 0")
        void zeroWithScale() {
            assertThat(BudgetEvaluator.evaluate(event("FREE"), new BigDecimal("0.00"))).isEqualTo(BudgetFit.WITHIN);
            assertThat(BudgetEvaluator.evaluate(event("KNOWN_20"), new BigDecimal("0.00"))).isEqualTo(BudgetFit.OVER);
        }

        @Test
        @DisplayName("a known price without currency cannot be compared: no budget = UNKNOWN, budget = OVER")
        void missingCurrency() {
            assertThat(BudgetEvaluator.evaluate(priced(BigDecimal.TEN, null), null)).isEqualTo(BudgetFit.UNKNOWN);
            assertThat(BudgetEvaluator.evaluate(priced(BigDecimal.TEN, null), new BigDecimal("25")))
                    .isEqualTo(BudgetFit.OVER);
        }

        @Test
        @DisplayName("currency comparison ignores case")
        void lowerCaseCurrency() {
            assertThat(BudgetEvaluator.evaluate(priced(BigDecimal.TEN, "usd"), new BigDecimal("25")))
                    .isEqualTo(BudgetFit.WITHIN);
        }
    }

    private static Event event(String price) {
        return switch (price) {
            case "FREE" -> priced(BigDecimal.ZERO, "USD");
            case "KNOWN_20" -> priced(new BigDecimal("20"), "USD");
            case "KNOWN_40" -> priced(new BigDecimal("40"), "USD");
            case "EUR" -> priced(BigDecimal.TEN, "EUR");
            case "UNKNOWN" -> TestEvents.event("e1").priceKnown(false).build();
            default -> throw new IllegalArgumentException(price);
        };
    }

    private static Event priced(BigDecimal min, String currency) {
        return TestEvents.event("e1").priceMin(min).priceMax(min).currency(currency).priceKnown(true).build();
    }
}
