package kz.astyq.zimran.planner.util;

import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.planner.model.enums.BudgetFit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetEvaluatorTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    /**
     * Price kinds: FREE (known, USD 0), KNOWN_20 / KNOWN_40 (known, USD), UNKNOWN, EUR (known 10, in EUR).
     */
    @ParameterizedTest(name = "budget {0}, price {1}, includeUnknown {2} -> {3}")
    @DisplayName("evaluate: budget x price matrix")
    @CsvSource(nullValues = "NULL", value = {
            "NULL, FREE,     true,  WITHIN",
            "NULL, KNOWN_20, true,  WITHIN",
            "NULL, KNOWN_40, true,  WITHIN",
            "NULL, UNKNOWN,  true,  UNKNOWN",
            "NULL, UNKNOWN,  false, UNKNOWN",
            "NULL, EUR,      true,  UNKNOWN",
            "NULL, EUR,      false, UNKNOWN",
            "0,    FREE,     true,  WITHIN",
            "0,    KNOWN_20, true,  OVER",
            "0,    KNOWN_40, true,  OVER",
            "0,    UNKNOWN,  true,  OVER",
            "0,    EUR,      true,  OVER",
            "25,   FREE,     true,  WITHIN",
            "25,   KNOWN_20, true,  WITHIN",
            "25,   KNOWN_40, true,  OVER",
            "25,   UNKNOWN,  true,  UNKNOWN",
            "25,   UNKNOWN,  false, OVER",
            "25,   EUR,      true,  UNKNOWN",
            "25,   EUR,      false, OVER"
    })
    void matrix(BigDecimal budget, String price, boolean includeUnknownPrice, BudgetFit expected) {
        assertThat(BudgetEvaluator.evaluate(event(price), budget, includeUnknownPrice)).isEqualTo(expected);
    }

    @Nested
    @DisplayName("evaluate: edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a minimum price equal to the budget fits")
        void priceEqualToBudget() {
            assertThat(BudgetEvaluator.evaluate(priced(new BigDecimal("25.00"), "USD"), new BigDecimal("25"), true))
                    .isEqualTo(BudgetFit.WITHIN);
        }

        @Test
        @DisplayName("budget 0.00 behaves like 0")
        void zeroWithScale() {
            assertThat(BudgetEvaluator.evaluate(event("FREE"), new BigDecimal("0.00"), true)).isEqualTo(BudgetFit.WITHIN);
            assertThat(BudgetEvaluator.evaluate(event("KNOWN_20"), new BigDecimal("0.00"), true)).isEqualTo(BudgetFit.OVER);
        }

        @Test
        @DisplayName("a known price without currency cannot be compared")
        void missingCurrency() {
            assertThat(BudgetEvaluator.evaluate(priced(BigDecimal.TEN, null), new BigDecimal("25"), true))
                    .isEqualTo(BudgetFit.UNKNOWN);
        }

        @Test
        @DisplayName("currency comparison ignores case")
        void lowerCaseCurrency() {
            assertThat(BudgetEvaluator.evaluate(priced(BigDecimal.TEN, "usd"), new BigDecimal("25"), true))
                    .isEqualTo(BudgetFit.WITHIN);
        }
    }

    private static Event event(String price) {
        return switch (price) {
            case "FREE" -> priced(BigDecimal.ZERO, "USD");
            case "KNOWN_20" -> priced(new BigDecimal("20"), "USD");
            case "KNOWN_40" -> priced(new BigDecimal("40"), "USD");
            case "EUR" -> priced(BigDecimal.TEN, "EUR");
            case "UNKNOWN" -> base().priceKnown(false).build();
            default -> throw new IllegalArgumentException(price);
        };
    }

    private static Event priced(BigDecimal min, String currency) {
        return base().priceMin(min).priceMax(min).currency(currency).priceKnown(true).build();
    }

    private static Event.EventBuilder base() {
        return Event.builder()
                .id(new EventId(EventSource.TICKETMASTER, "e1"))
                .title("Show")
                .sourceUrl("https://www.ticketmaster.com/e1")
                .startAt(OffsetDateTime.parse("2026-11-20T19:00:00-05:00"))
                .timezone(NEW_YORK)
                .status(EventStatus.SCHEDULED)
                .category("Music")
                .interests(List.of(Interest.MUSIC));
    }
}
