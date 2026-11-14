package kz.astyq.zimran.event.service.impl;

import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventCriteria;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.domain.EventSchedule;
import kz.astyq.zimran.event.model.domain.TestEvents;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.event.service.EventProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static kz.astyq.zimran.event.model.domain.TestEvents.NYC;
import static kz.astyq.zimran.event.model.domain.TestEvents.event;
import static kz.astyq.zimran.event.model.domain.TestEvents.eventAt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventQueryServiceImplTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 20);

    @Mock
    private EventProvider provider;

    private EventQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(provider.source()).thenReturn(EventSource.JSON);
        service = new EventQueryServiceImpl(List.of(provider));
    }

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("only events in the city (name ignoring case)")
        void city() {
            events(eventAt("ny", DAY, "19:00").city("new york ").build(),
                    eventAt("la", DAY, "19:00").city("Los Angeles").build());

            assertThat(ids(service.search(criteria().build()))).containsExactly("ny");
        }

        @Test
        @DisplayName("date bounds are days in New York, inclusive: 23:59 local is still the same day")
        void nyCalendarDay() {
            OffsetDateTime lateLocal = TestEvents.at(DAY, "23:59");
            OffsetDateTime utcNextDay = lateLocal.withOffsetSameInstant(ZoneOffset.UTC);
            events(event("late").startAt(utcNextDay).build(),
                    eventAt("next", DAY.plusDays(1), "00:00").build(),
                    eventAt("before", DAY.minusDays(1), "23:59").build());

            List<Event> found = service.search(criteria().fromDate(DAY).toDate(DAY).build());

            assertThat(utcNextDay.toLocalDate()).isEqualTo(DAY.plusDays(1));
            assertThat(ids(found)).containsExactly("late");
        }

        @Test
        @DisplayName("date-only events use their day; undated events only when includeUndated")
        void dateOnlyAndUndated() {
            events(event("date-only").localDate(DAY).build(), event("undated").dateText("Every Friday").build());

            assertThat(ids(service.search(criteria().fromDate(DAY).toDate(DAY).build()))).containsExactly("date-only");
            assertThat(ids(service.search(criteria().fromDate(DAY).includeUndated(true).build())))
                    .containsExactly("date-only", "undated");
        }

        @Test
        @DisplayName("interests: an event matches when any of its interests is requested; empty = all")
        void interests() {
            events(eventAt("music", DAY, "19:00").build(),
                    TestEvents.interests(eventAt("both", DAY, "19:30"), Interest.SPORTS, Interest.FOOD).build(),
                    TestEvents.interests(eventAt("art", DAY, "20:00"), Interest.ART).build());

            assertThat(ids(service.search(criteria().interests(Set.of(Interest.FOOD)).build()))).containsExactly("both");
            assertThat(ids(service.search(criteria().interests(Set.of()).build())))
                    .containsExactly("music", "both", "art");
        }

        @Test
        @DisplayName("keyword: case-insensitive substring of title, description or venue")
        void keyword() {
            events(eventAt("title", DAY, "18:00").title("JAZZ Night").build(),
                    eventAt("description", DAY, "19:00").description("Late-night jazz trio").build(),
                    eventAt("venue", DAY, "20:00").venue("Jazz Standard").build(),
                    eventAt("address", DAY, "21:00").address("1 Jazz St").build());

            assertThat(ids(service.search(criteria().keyword("  jazz ").build())))
                    .containsExactly("title", "description", "venue");
        }

        @Test
        @DisplayName("majorOnly keeps only major events")
        void major() {
            events(eventAt("big", DAY, "19:00").major(true).build(), eventAt("small", DAY, "20:00").build());

            assertThat(ids(service.search(criteria().majorOnly(true).build()))).containsExactly("big");
            assertThat(ids(service.search(criteria().build()))).containsExactly("big", "small");
        }

        @Test
        @DisplayName("sorted by start (date-only at the start of its day, undated last), then title")
        void sorting() {
            events(eventAt("b", DAY, "20:00").title("beta").build(),
                    event("undated").title("Aaa").build(),
                    eventAt("a", DAY, "20:00").title("Alpha").build(),
                    event("day").title("Zed").localDate(DAY).build(),
                    eventAt("early", DAY, "09:00").title("Omega").build());

            assertThat(ids(service.search(criteria().includeUndated(true).build())))
                    .containsExactly("day", "early", "a", "b", "undated");
        }

        @Test
        @DisplayName("duplicates are removed")
        void dedupe() {
            Event one = eventAt("one", DAY, "19:00").title("Show").venue("Hall").build();
            Event copy = eventAt("copy", DAY, "19:00").title("show!").venue("hall").build();
            events(one, copy, one);

            assertThat(ids(service.search(criteria().build()))).containsExactly("one");
        }

        @Test
        @DisplayName("a city name shared by two states: only the event in the city's own state")
        void sameNameOtherState() {
            events(eventAt("ny", DAY, "19:00").state("NY").build(),
                    eventAt("other", DAY, "19:00").state("TX").build(),
                    eventAt("no-state", DAY, "19:00").build());

            assertThat(ids(service.search(criteria().build()))).containsExactlyInAnyOrder("ny", "no-state");
        }

        @Test
        @DisplayName("a multi-day event matches any of its days and sorts by its first day in the range")
        void multiDay() {
            EventSchedule fourDays = EventSchedule.builder().firstDate(DAY.minusDays(10)).lastDate(DAY.plusDays(3)).build();
            EventSchedule weekends = EventSchedule.builder().dayOfWeek(DayOfWeek.SATURDAY).dayOfWeek(DayOfWeek.SUNDAY).build();
            events(event("running").schedule(fourDays).build(),
                    event("weekends").schedule(weekends).build(),
                    event("ended").schedule(EventSchedule.builder()
                            .firstDate(DAY.minusDays(10)).lastDate(DAY.minusDays(1)).build()).build(),
                    eventAt("later", DAY.plusDays(1), "09:00").build());

            // DAY is a Friday: the exhibition is on that day, the weekend event first on Saturday.
            assertThat(DAY.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
            assertThat(ids(service.search(criteria().fromDate(DAY).toDate(DAY.plusDays(2)).build())))
                    .containsExactly("running", "later", "weekends");
            assertThat(ids(service.search(criteria().fromDate(DAY).toDate(DAY).build())))
                    .containsExactly("running");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("asks the provider of the id's source")
        void found() {
            Event event = event("e1").build();
            when(provider.findById("e1")).thenReturn(Optional.of(event));

            assertThat(service.findById(new EventId(EventSource.JSON, "e1"))).contains(event);
        }

        @Test
        @DisplayName("unknown id or null: empty")
        void missing() {
            when(provider.findById("nope")).thenReturn(Optional.empty());

            assertThat(service.findById(new EventId(EventSource.JSON, "nope"))).isEmpty();
            assertThat(service.findById(null)).isEmpty();
            verify(provider, never()).getAll();
        }
    }

    @Test
    @DisplayName("coveredInterests: every interest of the city's events")
    void coveredInterests() {
        events(TestEvents.interests(event("a"), Interest.MUSIC, Interest.FOOD).build(),
                TestEvents.interests(event("b"), Interest.ART).city("Boston").build());

        assertThat(service.coveredInterests(NYC)).isEqualTo(EnumSet.of(Interest.MUSIC, Interest.FOOD));
    }

    private void events(Event... events) {
        when(provider.getAll()).thenReturn(new ArrayList<>(List.of(events)));
    }

    private static EventCriteria.EventCriteriaBuilder criteria() {
        return EventCriteria.builder().city(NYC);
    }

    private static List<String> ids(List<Event> events) {
        return events.stream().map(e -> e.getId().getSourceId()).toList();
    }
}
