package kz.astyq.zimran.event.util;

import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.TestEvents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventDeduplicatorTest {

    private static final OffsetDateTime EIGHT_PM = OffsetDateTime.parse("2026-11-20T20:00:00-05:00");

    @Nested
    @DisplayName("dedupe")
    class Dedupe {

        @Test
        @DisplayName("same EventId: one event kept")
        void sameId() {
            Event first = event("a", "Indie Night", "Brooklyn Steel", EIGHT_PM, false);
            Event again = event("a", "Indie Night", "Brooklyn Steel", EIGHT_PM, false);

            assertThat(EventDeduplicator.dedupe(List.of(first, again))).containsExactly(first);
        }

        @Test
        @DisplayName("same EventId: the one with a known price wins")
        void sameIdPriceKnownWins() {
            Event unknown = event("a", "Indie Night", "Brooklyn Steel", EIGHT_PM, false);
            Event priced = event("a", "Indie Night", "Brooklyn Steel", EIGHT_PM, true);

            assertThat(EventDeduplicator.dedupe(List.of(unknown, priced))).containsExactly(priced);
        }

        @Test
        @DisplayName("different ids, same title ignoring case and punctuation, same venue and minute")
        void sameListing() {
            Event first = event("a", "Indie Night!", "Brooklyn Steel", EIGHT_PM, false);
            Event second = event("b", "indie   night", "BROOKLYN STEEL", EIGHT_PM.plusSeconds(30), true);
            Event third = event("c", "Indie-Night", "brooklyn steel", EIGHT_PM, true);

            assertThat(EventDeduplicator.dedupe(List.of(first, second, third))).containsExactly(second);
        }

        @Test
        @DisplayName("same instant written with a different offset is the same minute")
        void sameInstantOtherOffset() {
            Event est = event("a", "Indie Night", "Brooklyn Steel", EIGHT_PM, true);
            Event utc = event("b", "Indie Night", "Brooklyn Steel",
                    EIGHT_PM.atZoneSameInstant(ZoneId.of("UTC")).toOffsetDateTime(), true);

            assertThat(EventDeduplicator.dedupe(List.of(est, utc))).containsExactly(est);
        }

        @Test
        @DisplayName("neither has a known price: the first is kept")
        void firstKept() {
            Event first = event("a", "Indie Night", null, EIGHT_PM, false);
            Event second = event("b", "Indie Night", null, EIGHT_PM, false);

            assertThat(EventDeduplicator.dedupe(List.of(first, second))).containsExactly(first);
        }

        @Test
        @DisplayName("different minute, venue or title: all kept, in input order")
        void distinct() {
            Event base = event("a", "Indie Night", "Brooklyn Steel", EIGHT_PM, true);
            Event otherMinute = event("b", "Indie Night", "Brooklyn Steel", EIGHT_PM.plusMinutes(1), true);
            Event otherVenue = event("c", "Indie Night", "Webster Hall", EIGHT_PM, true);
            Event otherTitle = event("d", "Indie Night II", "Brooklyn Steel", EIGHT_PM, true);
            Event noVenue = event("e", "Indie Night", null, EIGHT_PM, true);

            assertThat(EventDeduplicator.dedupe(List.of(base, otherMinute, otherVenue, otherTitle, noVenue)))
                    .containsExactly(base, otherMinute, otherVenue, otherTitle, noVenue);
        }

        @Test
        @DisplayName("null and empty input, null entries: skipped")
        void nothing() {
            assertThat(EventDeduplicator.dedupe(null)).isEmpty();
            assertThat(EventDeduplicator.dedupe(List.of())).isEmpty();
            List<Event> withNull = new ArrayList<>();
            withNull.add(null);
            Event event = event("a", "Indie Night", null, EIGHT_PM, false);
            withNull.add(event);
            assertThat(EventDeduplicator.dedupe(withNull)).containsExactly(event);
        }
    }

    @Nested
    @DisplayName("dedupe: events without a start time")
    class WithoutTime {

        @Test
        @DisplayName("same title, venue and day without time: one kept")
        void sameDay() {
            Event first = TestEvents.event("a").title("Book Fair").venue("Hall").localDate(EIGHT_PM.toLocalDate()).build();
            Event second = TestEvents.event("b").title("book fair").venue("hall").localDate(EIGHT_PM.toLocalDate()).build();

            assertThat(EventDeduplicator.dedupe(List.of(first, second))).containsExactly(first);
        }

        @Test
        @DisplayName("a dated and an undated listing are different; undated ones match only by the same date text")
        void undated() {
            Event dated = TestEvents.event("a").title("Book Fair").localDate(EIGHT_PM.toLocalDate()).build();
            Event undated = TestEvents.event("b").title("Book Fair").dateText("Every Sunday").build();
            Event sameText = TestEvents.event("c").title("Book Fair").dateText("every  sunday").build();
            Event otherText = TestEvents.event("d").title("Book Fair").dateText("Mondays").build();

            assertThat(EventDeduplicator.dedupe(List.of(dated, undated, sameText, otherText)))
                    .containsExactly(dated, undated, otherText);
        }
    }

    @Nested
    @DisplayName("contentKey")
    class ContentKey {

        @Test
        @DisplayName("lowercased title without punctuation, lowercased venue, UTC minute")
        void key() {
            Event event = event("a", "  Rock & Roll: Live!  ", "Madison Square Garden",
                    OffsetDateTime.parse("2026-11-20T20:00:59-05:00"), true);

            assertThat(EventDeduplicator.contentKey(event))
                    .isEqualTo("rock roll live|madison square garden|2026-11-21T01:00:00Z");
        }
    }

    private static Event event(String id, String title, String venue, OffsetDateTime startAt, boolean priceKnown) {
        return TestEvents.event(id)
                .title(title)
                .startAt(startAt)
                .venue(venue)
                .priceKnown(priceKnown)
                .priceMin(priceKnown ? new BigDecimal("20") : null)
                .priceMax(priceKnown ? new BigDecimal("40") : null)
                .currency(priceKnown ? "USD" : null)
                .build();
    }
}
