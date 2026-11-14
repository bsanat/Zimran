package kz.astyq.zimran.planner.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.core.configuration.PlannerProperties;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventCriteria;
import kz.astyq.zimran.event.model.domain.TestEvents;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.event.service.EventQueryService;
import kz.astyq.zimran.planner.model.domain.Candidate;
import kz.astyq.zimran.planner.model.domain.CandidateSet;
import kz.astyq.zimran.planner.model.domain.FilterStats;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.model.enums.BudgetFit;
import kz.astyq.zimran.planner.model.enums.TimeOfDay;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import kz.astyq.zimran.planner.service.HintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateServiceImplTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final SupportedCity NYC = SupportedCity.builder()
            .id("nyc").name("New York").stateCode("NY").countryCode("US").zone(NEW_YORK).build();
    /** 2026-11-14T10:00 in New York. */
    private static final Clock CLOCK = Clock.fixed(
            LocalDateTime.of(2026, 11, 14, 10, 0).atZone(NEW_YORK).toInstant(), NEW_YORK);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);

    @Mock
    private EventQueryService eventQueryService;
    @Mock
    private HintService hintService;

    private PlannerProperties plannerProperties;
    private CandidateServiceImpl service;

    @BeforeEach
    void setUp() {
        plannerProperties = new PlannerProperties();
        service = new CandidateServiceImpl(eventQueryService, hintService, plannerProperties, CLOCK);
    }

    @Nested
    @DisplayName("buildCandidates")
    class BuildCandidates {

        @Test
        @DisplayName("queries the city's dated events for the requested range and interests")
        void queriesEvents() {
            ValidatedPlanRequest request = request().endDate(TODAY.plusDays(2)).build();
            when(eventQueryService.search(any())).thenReturn(List.of());
            when(hintService.buildHints(any(), any())).thenReturn(List.of("hint"));

            CandidateSet result = service.buildCandidates(request);

            ArgumentCaptor<EventCriteria> criteria = ArgumentCaptor.forClass(EventCriteria.class);
            verify(eventQueryService).search(criteria.capture());
            assertThat(criteria.getValue().getCity()).isEqualTo(NYC);
            assertThat(criteria.getValue().getFromDate()).isEqualTo(TODAY);
            assertThat(criteria.getValue().getToDate()).isEqualTo(TODAY.plusDays(2));
            assertThat(criteria.getValue().getInterests()).containsExactly(Interest.MUSIC);
            assertThat(criteria.getValue().isIncludeUndated()).isFalse();
            assertThat(result.getCandidates()).isEmpty();
            assertThat(result.getStats().getTotal()).isZero();
            assertThat(result.getHints()).containsExactly("hint");
        }

        @Test
        @DisplayName("cancelled and postponed events are excluded")
        void notScheduledExcluded() {
            ValidatedPlanRequest request = request().build();
            events(event("ok", "Open", at(TODAY, "19:00"), "Hall").build(),
                    event("c", "Cancelled", at(TODAY, "19:00"), "Hall").status(EventStatus.CANCELLED).build(),
                    event("p", "Postponed", at(TODAY, "19:00"), "Hall").status(EventStatus.POSTPONED).build());

            CandidateSet result = service.buildCandidates(request);

            assertThat(ids(result)).containsExactly("ok");
            assertThat(result.getStats().getNotScheduled()).isEqualTo(2);
        }

        @Test
        @DisplayName("every filter is counted once, in order, and the survivors carry their facts")
        void statsCounts() {
            ValidatedPlanRequest request = request()
                    .endDate(TODAY.plusDays(1))
                    .interests(Set.of(Interest.MUSIC))
                    .budgetMax(new BigDecimal("25"))
                    .timePreference(TimePreference.EVENING)
                    .build();
            events(// cancelled and also past: counted as notScheduled only
                    event("cancelled-past", "A", at(TODAY, "08:00"), "V1").status(EventStatus.CANCELLED).build(),
                    event("past", "B", at(TODAY, "09:00"), "V1").build(),
                    event("outside", "C", at(TODAY.plusDays(2), "19:00"), "V1").build(),
                    event("sports", "D", at(TODAY, "19:00"), "V1").interests(Interest.SPORTS).build(),
                    event("afternoon", "E", at(TODAY, "13:00"), "V1").build(),
                    event("pricey", "F", at(TODAY, "19:00"), "V1").price("40").build(),
                    event("cheap", "G", at(TODAY, "19:00"), "V2").price("20").build(),
                    event("unknown", "H", at(TODAY.plusDays(1), "18:00"), "V3").build());
            when(hintService.buildHints(any(), any())).thenReturn(List.of());

            CandidateSet result = service.buildCandidates(request);

            assertThat(result.getStats()).usingRecursiveComparison().isEqualTo(FilterStats.builder()
                    .total(8).notScheduled(1).past(1).outsideRange(1).noInterestMatch(1).wrongTimeOfDay(1)
                    .overBudget(2).build());
            assertThat(ids(result)).containsExactly("cheap");
            Candidate cheap = result.getCandidates().getFirst();
            assertThat(cheap.getBudgetFit()).isEqualTo(BudgetFit.WITHIN);
            assertThat(cheap.getTimeOfDay()).isEqualTo(TimeOfDay.EVENING);
            assertThat(cheap.getMatchedInterests()).containsExactly(Interest.MUSIC);
        }

        @Test
        @DisplayName("with a budget an unknown price never passes; without one it stays as UNKNOWN")
        void unknownPrice() {
            ValidatedPlanRequest withBudget = request().budgetMax(new BigDecimal("25")).build();
            events(event("unknown", "A", at(TODAY, "19:00"), "V").build());
            when(hintService.buildHints(any(), any())).thenReturn(List.of());

            CandidateSet limited = service.buildCandidates(withBudget);
            CandidateSet unlimited = service.buildCandidates(request().build());

            assertThat(limited.getCandidates()).isEmpty();
            assertThat(limited.getStats().getOverBudget()).isEqualTo(1);
            assertThat(unlimited.getCandidates()).singleElement()
                    .extracting(Candidate::getBudgetFit).isEqualTo(BudgetFit.UNKNOWN);
        }

        @Test
        @DisplayName("free only (budget 0): only a known USD price of 0 passes")
        void freeOnly() {
            ValidatedPlanRequest request = request().budgetMax(BigDecimal.ZERO).build();
            events(event("free", "A", at(TODAY, "19:00"), "V1").price("0").build(),
                    event("paid", "B", at(TODAY, "19:00"), "V2").price("5").build(),
                    event("unknown", "C", at(TODAY, "19:00"), "V3").build());
            when(hintService.buildHints(any(), any())).thenReturn(List.of());

            CandidateSet result = service.buildCandidates(request);

            assertThat(ids(result)).containsExactly("free");
            assertThat(result.getStats().getOverBudget()).isEqualTo(2);
        }

        @Test
        @DisplayName("an event without a known time passes ANY but no specific time of day")
        void unknownTime() {
            Event dateOnly = TestEvents.event("date-only").localDate(TODAY).venue("V").build();
            events(dateOnly);
            when(hintService.buildHints(any(), any())).thenReturn(List.of());

            CandidateSet any = service.buildCandidates(request().build());
            CandidateSet evening = service.buildCandidates(request().timePreference(TimePreference.EVENING).build());

            assertThat(any.getCandidates()).singleElement().satisfies(candidate ->
                    assertThat(candidate.getTimeOfDay()).isNull());
            assertThat(evening.getCandidates()).isEmpty();
            assertThat(evening.getStats().getWrongTimeOfDay()).isEqualTo(1);
        }

        @Test
        @DisplayName("time of day is read in the event's timezone")
        void timeOfDayInEventZone() {
            ValidatedPlanRequest request = request().timePreference(TimePreference.MORNING).build();
            // 16:00 UTC = 11:00 in New York
            OffsetDateTime utc = at(TODAY, "11:00").withOffsetSameInstant(ZoneOffset.UTC);
            Event utcStart = event("u", "B", utc, "W").build();
            events(utcStart);
            when(hintService.buildHints(any(), any())).thenReturn(List.of());

            CandidateSet result = service.buildCandidates(request);

            assertThat(ids(result)).containsExactly("u");
            assertThat(result.getCandidates().getFirst().getTimeOfDay()).isEqualTo(TimeOfDay.MORNING);
        }

        @Test
        @DisplayName("ordered by preScore desc, then start, then title")
        void ordering() {
            ValidatedPlanRequest request = request().endDate(TODAY.plusDays(2)).build();
            events(event("late-day", "Zed", at(TODAY.plusDays(2), "19:00"), "V1").build(),
                    event("b-title", "beta", at(TODAY, "20:00"), "V2").build(),
                    event("a-title", "Alpha", at(TODAY, "20:00"), "V3").build(),
                    event("earlier", "Omega", at(TODAY, "18:00"), "V4").build(),
                    event("rich", "Rich", at(TODAY.plusDays(1), "19:00"), "V5")
                            .imageUrl("https://img/1.jpg").description("Live").price("10").build());

            CandidateSet result = service.buildCandidates(request);

            assertThat(ids(result)).containsExactly("rich", "earlier", "a-title", "b-title", "late-day");
            assertThat(result.getCandidates()).extracting(Candidate::getPreScore)
                    .isSortedAccordingTo((a, b) -> Integer.compare(b, a));
            verifyNoInteractions(hintService);
        }

        @Test
        @DisplayName("keeps at most 3 events per venue per day, the best ones")
        void venueCap() {
            ValidatedPlanRequest request = request().endDate(TODAY.plusDays(1)).build();
            events(event("h1", "A", at(TODAY, "18:00"), "Hall").build(),
                    event("h2", "B", at(TODAY, "19:00"), "HALL ").build(),
                    event("h3", "C", at(TODAY, "20:00"), "hall").build(),
                    event("h4", "D", at(TODAY, "21:00"), "Hall").build(),
                    event("h-best", "E", at(TODAY, "22:00"), "Hall").imageUrl("https://img/1.jpg").build(),
                    event("next-day", "F", at(TODAY.plusDays(1), "19:00"), "Hall").build(),
                    event("other", "G", at(TODAY, "19:00"), "Club").build(),
                    event("n1", "H", at(TODAY, "19:00"), null).build(),
                    event("n2", "I", at(TODAY, "19:30"), null).build(),
                    event("n3", "J", at(TODAY, "20:00"), null).build(),
                    event("n4", "K", at(TODAY, "20:30"), null).build());

            CandidateSet result = service.buildCandidates(request);

            assertThat(ids(result))
                    .contains("h-best", "h1", "h2", "next-day", "other", "n1", "n2", "n3", "n4")
                    .doesNotContain("h3", "h4");
            assertThat(result.getStats().getTotal()).isEqualTo(11);
        }

        @Test
        @DisplayName("caps the result at maxCandidates (30) and adds no hints")
        void maxCandidates() {
            ValidatedPlanRequest request = request().build();
            events(IntStream.range(0, 40)
                    .mapToObj(i -> event("e" + i, "Event %02d".formatted(i), at(TODAY, "19:00"), "Venue " + i).build())
                    .toArray(Event[]::new));

            CandidateSet result = service.buildCandidates(request);

            assertThat(result.getCandidates()).hasSize(30);
            assertThat(result.getCandidates().getFirst().getEvent().getTitle()).isEqualTo("Event 00");
            assertThat(result.getHints()).isEmpty();
            assertThat(result.getStats().getTotal()).isEqualTo(40);
            verifyNoInteractions(hintService);
        }

        @Test
        @DisplayName("asks for hints with the stats when fewer than 3 candidates remain")
        void hintsWhenFew() {
            ValidatedPlanRequest request = request().build();
            events(event("a", "A", at(TODAY, "19:00"), "V1").build(),
                    event("b", "B", at(TODAY, "19:00"), "V2").build(),
                    event("x", "X", at(TODAY, "19:00"), "V3").status(EventStatus.CANCELLED).build());
            when(hintService.buildHints(any(), any())).thenReturn(List.of("Try different dates."));

            CandidateSet result = service.buildCandidates(request);

            assertThat(result.getHints()).containsExactly("Try different dates.");
            verify(hintService).buildHints(result.getStats(), request);
        }
    }

    private void events(Event... events) {
        when(eventQueryService.search(any())).thenReturn(new ArrayList<>(List.of(events)));
    }

    private static List<String> ids(CandidateSet result) {
        return result.getCandidates().stream().map(c -> c.getEvent().getId().getSourceId()).toList();
    }

    private static ValidatedPlanRequest.ValidatedPlanRequestBuilder request() {
        return ValidatedPlanRequest.builder()
                .city(NYC)
                .interests(Set.of(Interest.MUSIC))
                .startDate(TODAY)
                .endDate(TODAY)
                .timePreference(TimePreference.ANY);
    }

    private static OffsetDateTime at(LocalDate date, String time) {
        return LocalDateTime.of(date, LocalTime.parse(time)).atZone(NEW_YORK).toOffsetDateTime();
    }

    private static TestEvent event(String id, String title, OffsetDateTime startAt, String venue) {
        return new TestEvent(TestEvents.event(id)
                .title(title)
                .startAt(startAt)
                .localDate(startAt.atZoneSameInstant(NEW_YORK).toLocalDate())
                .venue(venue));
    }

    /** Thin wrapper over {@link Event.EventBuilder} so tests can set a USD price in one call. */
    private static final class TestEvent {
        private final Event.EventBuilder builder;

        private TestEvent(Event.EventBuilder builder) {
            this.builder = builder;
        }

        TestEvent status(EventStatus status) {
            builder.status(status);
            return this;
        }

        TestEvent interests(Interest interest) {
            TestEvents.interests(builder, interest);
            return this;
        }

        TestEvent imageUrl(String imageUrl) {
            builder.imageUrl(imageUrl);
            return this;
        }

        TestEvent description(String description) {
            builder.description(description);
            return this;
        }

        TestEvent price(String min) {
            TestEvents.price(builder, min, min);
            return this;
        }

        Event build() {
            return builder.build();
        }
    }
}
