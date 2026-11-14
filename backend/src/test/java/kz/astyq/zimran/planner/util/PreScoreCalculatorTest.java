package kz.astyq.zimran.planner.util;

import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.TestEvents;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.planner.model.enums.BudgetFit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PreScoreCalculatorTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final LocalDate START = LocalDate.of(2026, 11, 14);
    private static final LocalDate END = LocalDate.of(2026, 11, 20);

    @Nested
    @DisplayName("score")
    class Score {

        @Test
        @DisplayName("a perfect event on the first day scores 100")
        void perfect() {
            Set<Interest> music = Set.of(Interest.MUSIC);

            int score = PreScoreCalculator.score(rich(), music, music, BudgetFit.WITHIN, true, START, START, END);

            assertThat(score).isEqualTo(100);
        }

        @Test
        @DisplayName("a bare event on the last day with nothing matching scores 0")
        void nothing() {
            int score = PreScoreCalculator.score(bare(), Set.of(), Set.of(Interest.MUSIC), BudgetFit.OVER, false,
                    END, START, END);

            assertThat(score).isZero();
        }

        @Test
        @DisplayName("interest overlap is 40 x matched / requested")
        void interestOverlap() {
            Set<Interest> requested = EnumSet.of(Interest.MUSIC, Interest.COMEDY);

            assertThat(onlyInterests(EnumSet.of(Interest.MUSIC, Interest.COMEDY), requested)).isEqualTo(40);
            assertThat(onlyInterests(EnumSet.of(Interest.MUSIC), requested)).isEqualTo(20);
        }

        @Test
        @DisplayName("one match is worth at least 20 even with many requested interests")
        void interestMinimum() {
            Set<Interest> requested = EnumSet.of(Interest.MUSIC, Interest.COMEDY, Interest.ART, Interest.SPORTS);

            assertThat(onlyInterests(EnumSet.of(Interest.MUSIC), requested)).isEqualTo(20);
            assertThat(onlyInterests(EnumSet.of(Interest.MUSIC, Interest.COMEDY, Interest.ART), requested))
                    .isEqualTo(30);
        }

        @Test
        @DisplayName("matched interests outside the request do not count")
        void interestOutsideRequest() {
            assertThat(onlyInterests(EnumSet.of(Interest.SPORTS), EnumSet.of(Interest.MUSIC))).isZero();
        }

        @Test
        @DisplayName("budget: WITHIN +20, UNKNOWN +5, OVER 0")
        void budget() {
            assertThat(onlyBudget(BudgetFit.WITHIN)).isEqualTo(20);
            assertThat(onlyBudget(BudgetFit.UNKNOWN)).isEqualTo(5);
            assertThat(onlyBudget(BudgetFit.OVER)).isZero();
        }

        @Test
        @DisplayName("time match +15, image +5, description +5")
        void flags() {
            Set<Interest> none = Set.of();
            Set<Interest> music = Set.of(Interest.MUSIC);
            Event withImage = base().imageUrl("https://img/1.jpg").build();
            Event withDescription = base().description("Live").build();

            assertThat(PreScoreCalculator.score(bare(), none, music, BudgetFit.OVER, true, END, START, END)).isEqualTo(15);
            assertThat(PreScoreCalculator.score(withImage, none, music, BudgetFit.OVER, false, END, START, END)).isEqualTo(5);
            assertThat(PreScoreCalculator.score(withDescription, none, music, BudgetFit.OVER, false, END, START, END))
                    .isEqualTo(5);
            assertThat(PreScoreCalculator.score(base().description("  ").imageUrl("").build(), none, music,
                    BudgetFit.OVER, false, END, START, END)).isZero();
        }

        @Test
        @DisplayName("soonness falls linearly from 15 on the first day to 0 on the last")
        void soonness() {
            assertThat(onlySoonness(START, START, END)).isEqualTo(15);
            assertThat(onlySoonness(START.plusDays(3), START, END)).isEqualTo(8); // 7.5 rounded
            assertThat(onlySoonness(END, START, END)).isZero();
            assertThat(onlySoonness(START, START, START)).isEqualTo(15);
            assertThat(onlySoonness(END.plusDays(1), START, END)).isZero();
        }
    }

    private static int onlyInterests(Set<Interest> matched, Set<Interest> requested) {
        return PreScoreCalculator.score(bare(), matched, requested, BudgetFit.OVER, false, END, START, END);
    }

    private static int onlyBudget(BudgetFit fit) {
        return PreScoreCalculator.score(bare(), Set.of(), Set.of(Interest.MUSIC), fit, false, END, START, END);
    }

    private static int onlySoonness(LocalDate eventDate, LocalDate start, LocalDate end) {
        return PreScoreCalculator.score(bare(), Set.of(), Set.of(Interest.MUSIC), BudgetFit.OVER, false,
                eventDate, start, end);
    }

    private static Event rich() {
        return base().imageUrl("https://img/1.jpg").description("Live music").build();
    }

    private static Event bare() {
        return base().build();
    }

    private static Event.EventBuilder base() {
        return TestEvents.event("e1")
                .title("Show")
                .startAt(OffsetDateTime.parse("2026-11-20T19:00:00-05:00"));
    }
}
