package kz.astyq.zimran.planner.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.core.i18n.MessageService;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.planner.model.domain.FilterStats;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import kz.astyq.zimran.planner.util.MessageCode;
import kz.astyq.zimran.ticketmaster.service.InterestMappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HintServiceImplTest {

    private static final SupportedCity NYC = SupportedCity.builder()
            .id("nyc").name("New York").stateCode("NY").countryCode("US")
            .zone(ZoneId.of("America/New_York")).build();
    private static final LocalDate DAY = LocalDate.of(2026, 11, 14);

    @Mock
    private MessageService messageService;
    @Mock
    private InterestMappingService interestMappingService;

    @InjectMocks
    private HintServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(messageService.getMessage(anyString())).thenAnswer(inv -> "msg:" + inv.getArgument(0));
        lenient().when(interestMappingService.hasCoverage(any())).thenReturn(true);
    }

    @Nested
    @DisplayName("buildHints")
    class BuildHints {

        @Test
        @DisplayName("hints follow the filters that removed the most events, at most 3")
        void byImpact() {
            FilterStats stats = stats().past(1).noInterestMatch(7).overBudget(5).wrongTimeOfDay(2).build();

            assertThat(service.buildHints(stats, request(Set.of(Interest.MUSIC)))).containsExactly(
                    "msg:" + MessageCode.HINT_ADD_INTERESTS,
                    "msg:" + MessageCode.HINT_RAISE_BUDGET,
                    "msg:" + MessageCode.HINT_CHANGE_TIME);
        }

        @Test
        @DisplayName("cancelled, past and out-of-range events add up to the change-date hint")
        void changeDateAggregates() {
            FilterStats stats = stats().notScheduled(2).past(2).outsideRange(2).overBudget(5).build();

            assertThat(service.buildHints(stats, request(Set.of(Interest.MUSIC)))).containsExactly(
                    "msg:" + MessageCode.HINT_CHANGE_DATE,
                    "msg:" + MessageCode.HINT_RAISE_BUDGET);
        }

        @Test
        @DisplayName("filters that removed nothing give no hint; ties keep a fixed order")
        void zeroSkippedAndTies() {
            FilterStats stats = stats().wrongTimeOfDay(3).overBudget(3).build();

            assertThat(service.buildHints(stats, request(Set.of(Interest.MUSIC)))).containsExactly(
                    "msg:" + MessageCode.HINT_CHANGE_TIME,
                    "msg:" + MessageCode.HINT_RAISE_BUDGET);
        }

        @Test
        @DisplayName("each requested interest without coverage gets a low-coverage hint first")
        void lowCoverage() {
            when(interestMappingService.hasCoverage(Interest.FOOD)).thenReturn(false);
            when(messageService.getMessage(MessageCode.HINT_LOW_COVERAGE, "food", "New York"))
                    .thenReturn("We currently have few food events in New York.");

            List<String> hints = service.buildHints(stats().overBudget(1).build(),
                    request(EnumSet.of(Interest.MUSIC, Interest.FOOD)));

            assertThat(hints).containsExactly(
                    "We currently have few food events in New York.",
                    "msg:" + MessageCode.HINT_RAISE_BUDGET);
        }

        @Test
        @DisplayName("low-coverage hints count towards the limit of 3")
        void limitIncludesLowCoverage() {
            when(interestMappingService.hasCoverage(any())).thenReturn(false);
            lenient().when(messageService.getMessage(anyString(), any(), any()))
                    .thenAnswer(inv -> "low:" + inv.getArgument(1));

            List<String> hints = service.buildHints(stats().overBudget(9).build(),
                    request(EnumSet.of(Interest.MUSIC, Interest.FOOD, Interest.GAMING, Interest.ART)));

            assertThat(hints).containsExactly("low:music", "low:art", "low:food");
        }

        @Test
        @DisplayName("with nothing to explain, suggests other dates")
        void fallback() {
            assertThat(service.buildHints(stats().build(), request(Set.of(Interest.MUSIC))))
                    .containsExactly("msg:" + MessageCode.HINT_CHANGE_DATE);
        }
    }

    private static FilterStats.FilterStatsBuilder stats() {
        return FilterStats.builder().total(20);
    }

    private static ValidatedPlanRequest request(Set<Interest> interests) {
        return ValidatedPlanRequest.builder()
                .city(NYC)
                .interests(interests)
                .startDate(DAY)
                .endDate(DAY)
                .timePreference(TimePreference.ANY)
                .build();
    }
}
