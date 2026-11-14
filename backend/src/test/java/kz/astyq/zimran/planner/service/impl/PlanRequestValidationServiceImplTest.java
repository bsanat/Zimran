package kz.astyq.zimran.planner.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.configuration.PlannerProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.model.dto.PlanCreateRequest;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import kz.astyq.zimran.planner.util.MessageCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanRequestValidationServiceImplTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final SupportedCity NYC = SupportedCity.builder()
            .id("nyc").name("New York").stateCode("NY").countryCode("US").zone(NEW_YORK).build();
    /** 2026-11-14T10:00 in New York. */
    private static final Clock CLOCK = Clock.fixed(
            LocalDateTime.of(2026, 11, 14, 10, 0).atZone(NEW_YORK).toInstant(), NEW_YORK);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);

    @Mock
    private SupportedCityService supportedCityService;

    private PlannerProperties plannerProperties;
    private PlanRequestValidationServiceImpl service;

    @BeforeEach
    void setUp() {
        plannerProperties = new PlannerProperties();
        service = new PlanRequestValidationServiceImpl(supportedCityService, plannerProperties, CLOCK);
    }

    @Nested
    @DisplayName("validate")
    class Validate {

        @BeforeEach
        void city() {
            lenient().when(supportedCityService.getByName("New York")).thenReturn(NYC);
        }

        @Test
        @DisplayName("a valid request is returned with defaults applied")
        void defaults() {
            ValidatedPlanRequest result = service.validate(valid().build());

            assertThat(result.getCity()).isEqualTo(NYC);
            assertThat(result.getInterests()).containsExactly(Interest.MUSIC);
            assertThat(result.getStartDate()).isEqualTo(TODAY);
            assertThat(result.getEndDate()).isEqualTo(TODAY);
            assertThat(result.getBudgetMax()).isNull();
            assertThat(result.getTimePreference()).isEqualTo(TimePreference.ANY);
            assertThat(result.getNotes()).isNull();
        }

        @Test
        @DisplayName("explicit values are kept")
        void explicitValues() {
            ValidatedPlanRequest result = service.validate(valid()
                    .endDate(TODAY.plusDays(2))
                    .budgetMax(new BigDecimal("49.99"))
                    .timePreference(TimePreference.EVENING)
                    .notes("Jazz please")
                    .build());

            assertThat(result.getEndDate()).isEqualTo(TODAY.plusDays(2));
            assertThat(result.getBudgetMax()).isEqualByComparingTo("49.99");
            assertThat(result.getTimePreference()).isEqualTo(TimePreference.EVENING);
            assertThat(result.getNotes()).isEqualTo("Jazz please");
        }

        @Test
        @DisplayName("an unsupported city fails with the city service's error")
        void unsupportedCity() {
            ServiceValidationException error = new ServiceValidationException(ErrorCode.VALIDATION_ERROR,
                    "city_not_supported").withDetail("field", "city");
            when(supportedCityService.getByName("Paris")).thenThrow(error);

            assertThatThrownBy(() -> service.validate(valid().city("Paris").build())).isSameAs(error);
        }

        @Test
        @DisplayName("interests are deduplicated and nulls ignored")
        void interestsDeduplicated() {
            ValidatedPlanRequest result = service.validate(valid()
                    .interests(Arrays.asList(Interest.COMEDY, null, Interest.MUSIC, Interest.COMEDY))
                    .build());

            assertThat(result.getInterests()).containsExactlyInAnyOrder(Interest.MUSIC, Interest.COMEDY);
        }

        @Test
        @DisplayName("missing, empty or all-null interests are rejected")
        void interestsRequired() {
            assertInvalid(valid().interests(null), MessageCode.INTERESTS_REQUIRED, "interests");
            assertInvalid(valid().interests(List.of()), MessageCode.INTERESTS_REQUIRED, "interests");
            assertInvalid(valid().interests(Arrays.asList((Interest) null)), MessageCode.INTERESTS_REQUIRED,
                    "interests");
        }

        @Test
        @DisplayName("startDate is required")
        void startDateRequired() {
            assertInvalid(valid().startDate(null), MessageCode.START_DATE_REQUIRED, "startDate");
        }

        @Test
        @DisplayName("startDate before today (city zone) is rejected")
        void startDateInPast() {
            assertInvalid(valid().startDate(TODAY.minusDays(1)), MessageCode.START_DATE_IN_PAST, "startDate");
        }

        @Test
        @DisplayName("today is taken in the city's timezone, not UTC")
        void todayInCityZone() {
            // 23:30 in New York is already the next day in UTC
            Clock lateEvening = Clock.fixed(LocalDateTime.of(2026, 11, 14, 23, 30).atZone(NEW_YORK).toInstant(),
                    ZoneOffset.UTC);
            service = new PlanRequestValidationServiceImpl(supportedCityService, plannerProperties, lateEvening);

            assertThat(service.validate(valid().startDate(TODAY).build()).getStartDate()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("startDate may be up to 90 days ahead")
        void startDateTooFar() {
            assertThat(service.validate(valid().startDate(TODAY.plusDays(90)).build()).getStartDate())
                    .isEqualTo(TODAY.plusDays(90));
            ServiceValidationException error = assertInvalid(valid().startDate(TODAY.plusDays(91)),
                    MessageCode.START_DATE_TOO_FAR, "startDate");
            assertThat(error.getArguments()).containsExactly(90);
        }

        @Test
        @DisplayName("endDate before startDate is rejected")
        void endDateBeforeStart() {
            assertInvalid(valid().startDate(TODAY.plusDays(1)).endDate(TODAY), MessageCode.END_DATE_BEFORE_START,
                    "endDate");
        }

        @Test
        @DisplayName("the range may span maxRangeDays days inclusive")
        void rangeTooLong() {
            assertThat(service.validate(valid().endDate(TODAY.plusDays(6)).build()).getEndDate())
                    .isEqualTo(TODAY.plusDays(6));
            ServiceValidationException error = assertInvalid(valid().endDate(TODAY.plusDays(7)),
                    MessageCode.DATE_RANGE_TOO_LONG, "endDate");
            assertThat(error.getArguments()).containsExactly(7);
        }

        @Test
        @DisplayName("maxRangeDays comes from the planner properties")
        void rangeFromProperties() {
            plannerProperties.setMaxRangeDays(2);

            assertThat(service.validate(valid().endDate(TODAY.plusDays(1)).build()).getEndDate())
                    .isEqualTo(TODAY.plusDays(1));
            assertInvalid(valid().endDate(TODAY.plusDays(2)), MessageCode.DATE_RANGE_TOO_LONG, "endDate");
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "0.00", "25", "25.5", "25.50", "10000", "10000.00", "25.500"})
        @DisplayName("budgetMax from 0 to 10000 with at most 2 decimals is accepted")
        void budgetValid(String budget) {
            assertThat(service.validate(valid().budgetMax(new BigDecimal(budget)).build()).getBudgetMax())
                    .isEqualByComparingTo(budget);
        }

        @ParameterizedTest
        @ValueSource(strings = {"-0.01", "-1", "10000.01", "20000", "25.555", "0.001"})
        @DisplayName("budgetMax out of range or with more than 2 decimals is rejected")
        void budgetInvalid(String budget) {
            assertInvalid(valid().budgetMax(new BigDecimal(budget)), MessageCode.BUDGET_INVALID, "budgetMax");
        }

        @Test
        @DisplayName("notes are trimmed, spaces collapsed and control characters except newline removed")
        void notesSanitized() {
            ValidatedPlanRequest result = service.validate(valid()
                    .notes("  Jazz \u0000 or\u0007   blues\r\nnear   Brooklyn  please\t ")
                    .build());

            assertThat(result.getNotes()).isEqualTo("Jazz or blues\nnear Brooklyn please");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\n\n", "\u0000\u0001", " \t "})
        @DisplayName("blank notes become null")
        void blankNotes(String notes) {
            assertThat(service.validate(valid().notes(notes).build()).getNotes()).isNull();
        }

        @Test
        @DisplayName("notes up to 500 characters after cleaning are accepted, longer are rejected")
        void notesTooLong() {
            assertThat(service.validate(valid().notes("a".repeat(500)).build()).getNotes()).hasSize(500);
            assertThat(service.validate(valid().notes("  " + "a".repeat(500) + "  ").build()).getNotes())
                    .hasSize(500);

            ServiceValidationException error = assertInvalid(valid().notes("a".repeat(501)),
                    MessageCode.NOTES_TOO_LONG, "notes");
            assertThat(error.getArguments()).containsExactly(500);
        }
    }

    private ServiceValidationException assertInvalid(PlanCreateRequest.PlanCreateRequestBuilder request,
                                                     String messageCode, String field) {
        Throwable thrown = catchThrowable(() -> service.validate(request.build()));
        assertThat(thrown).isInstanceOf(ServiceValidationException.class);
        ServiceValidationException error = (ServiceValidationException) thrown;
        assertThat(error.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(error.getMessageCode()).isEqualTo(messageCode);
        assertThat(error.getDetails()).containsEntry("field", field);
        return error;
    }

    private static PlanCreateRequest.PlanCreateRequestBuilder valid() {
        return PlanCreateRequest.builder()
                .city("New York")
                .interests(List.of(Interest.MUSIC))
                .startDate(TODAY);
    }
}
