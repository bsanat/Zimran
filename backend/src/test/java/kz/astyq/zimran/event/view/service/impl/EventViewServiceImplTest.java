package kz.astyq.zimran.event.view.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.mapper.EventMapper;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.dto.EventDetailsViewResponse;
import kz.astyq.zimran.event.model.dto.EventPageViewResponse;
import kz.astyq.zimran.event.model.dto.EventSearchRequest;
import kz.astyq.zimran.event.model.dto.EventViewResponse;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.event.service.EventQueryService;
import kz.astyq.zimran.event.util.MessageCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventViewServiceImplTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final SupportedCity NYC = SupportedCity.builder()
            .id("nyc").name("New York").stateCode("NY").countryCode("US").zone(NEW_YORK).build();
    /** 22:00 on 2026-11-14 in New York, already 2026-11-15 in UTC. */
    private static final Instant NOW = Instant.parse("2026-11-15T03:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);

    @Mock
    private EventQueryService eventQueryService;
    @Mock
    private SupportedCityService supportedCityService;

    private EventViewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EventViewServiceImpl(eventQueryService, supportedCityService,
                Mappers.getMapper(EventMapper.class), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Nested
    @DisplayName("getPage")
    class GetPage {

        @BeforeEach
        void city() {
            when(supportedCityService.getByName("New York")).thenReturn(NYC);
        }

        @Test
        @DisplayName("date is shorthand for startDate = endDate = date and wins over both")
        void dateShorthand() {
            LocalDate date = TODAY.plusDays(6);
            stubEvents(0);

            service.getPage(request().date(date).startDate(TODAY).endDate(TODAY.plusDays(1)).build());

            verify(eventQueryService).findEvents(NYC, date, date, Set.of());
        }

        @Test
        @DisplayName("no dates: today in the city's timezone (not UTC)")
        void defaultToday() {
            stubEvents(0);

            service.getPage(request().build());

            verify(eventQueryService).findEvents(NYC, TODAY, TODAY, Set.of());
        }

        @Test
        @DisplayName("startDate only: endDate defaults to startDate")
        void endDefaultsToStart() {
            stubEvents(0);

            service.getPage(request().startDate(TODAY.plusDays(2)).build());

            verify(eventQueryService).findEvents(NYC, TODAY.plusDays(2), TODAY.plusDays(2), Set.of());
        }

        @Test
        @DisplayName("7 days inclusive is allowed")
        void sevenDays() {
            stubEvents(0);

            service.getPage(request().startDate(TODAY).endDate(TODAY.plusDays(6)).build());

            verify(eventQueryService).findEvents(NYC, TODAY, TODAY.plusDays(6), Set.of());
        }

        @Test
        @DisplayName("range over 7 days: DATE_RANGE_TOO_LONG on endDate")
        void tooLong() {
            assertValidationError(() -> service.getPage(request().startDate(TODAY).endDate(TODAY.plusDays(7)).build()),
                    MessageCode.DATE_RANGE_TOO_LONG, "endDate");
            assertThatThrownBy(() -> service.getPage(request().startDate(TODAY).endDate(TODAY.plusDays(7)).build()))
                    .extracting("arguments").isEqualTo(new Object[]{7});
        }

        @Test
        @DisplayName("startDate before today: DATE_IN_PAST on startDate")
        void pastStartDate() {
            assertValidationError(() -> service.getPage(request().startDate(TODAY.minusDays(1)).endDate(TODAY).build()),
                    MessageCode.DATE_IN_PAST, "startDate");
        }

        @Test
        @DisplayName("date before today: DATE_IN_PAST on date")
        void pastDate() {
            assertValidationError(() -> service.getPage(request().date(TODAY.minusDays(1)).build()),
                    MessageCode.DATE_IN_PAST, "date");
        }

        @Test
        @DisplayName("endDate before startDate: DATE_RANGE_INVALID on endDate")
        void endBeforeStart() {
            assertValidationError(() -> service.getPage(request().startDate(TODAY.plusDays(3))
                            .endDate(TODAY.plusDays(2)).build()),
                    MessageCode.DATE_RANGE_INVALID, "endDate");
        }

        @Test
        @DisplayName("defaults page 1, pageSize 20; hasMore when more items remain")
        void firstPage() {
            stubEvents(45);

            EventPageViewResponse page = service.getPage(request().build());

            assertThat(page.getPage()).isEqualTo(1);
            assertThat(page.getPageSize()).isEqualTo(20);
            assertThat(page.getTotal()).isEqualTo(45);
            assertThat(page.isHasMore()).isTrue();
            assertThat(page.getItems()).extracting(EventViewResponse::getId)
                    .first().isEqualTo("ticketmaster:e0");
            assertThat(page.getItems()).hasSize(20);
        }

        @Test
        @DisplayName("middle, last and past-the-end pages")
        void pagination() {
            stubEvents(45);

            EventPageViewResponse second = service.getPage(request().page(2).pageSize(20).build());
            EventPageViewResponse last = service.getPage(request().page(3).pageSize(20).build());
            EventPageViewResponse beyond = service.getPage(request().page(4).pageSize(20).build());

            assertThat(second.getItems()).extracting(EventViewResponse::getId)
                    .containsExactlyElementsOf(IntStream.range(20, 40).mapToObj(i -> "ticketmaster:e" + i).toList());
            assertThat(second.isHasMore()).isTrue();
            assertThat(last.getItems()).hasSize(5);
            assertThat(last.isHasMore()).isFalse();
            assertThat(beyond.getItems()).isEmpty();
            assertThat(beyond.isHasMore()).isFalse();
            assertThat(beyond.getTotal()).isEqualTo(45);
        }

        @Test
        @DisplayName("exactly one full page: hasMore false")
        void exactPage() {
            stubEvents(20);

            assertThat(service.getPage(request().build()).isHasMore()).isFalse();
        }

        @Test
        @DisplayName("categories are passed as interests")
        void categories() {
            stubEvents(0);

            service.getPage(request().category(List.of(Interest.MUSIC, Interest.COMEDY, Interest.MUSIC)).build());

            verify(eventQueryService).findEvents(NYC, TODAY, TODAY, Set.of(Interest.MUSIC, Interest.COMEDY));
        }
    }

    @Nested
    @DisplayName("getPage: city")
    class GetPageCity {

        @Test
        @DisplayName("unsupported city: the city service's error, no event search")
        void unsupportedCity() {
            ServiceValidationException error = new ServiceValidationException(ErrorCode.VALIDATION_ERROR,
                    kz.astyq.zimran.city.util.MessageCode.CITY_NOT_SUPPORTED).withDetail("field", "city");
            when(supportedCityService.getByName("Paris")).thenThrow(error);

            assertThatThrownBy(() -> service.getPage(EventSearchRequest.builder().city("Paris").build()))
                    .isSameAs(error);
            verifyNoInteractions(eventQueryService);
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("known id: mapped details")
        void found() {
            Event event = event(0);
            when(eventQueryService.findById(new EventId(EventSource.TICKETMASTER, "e0"))).thenReturn(Optional.of(event));

            EventDetailsViewResponse details = service.getById("ticketmaster:e0");

            assertThat(details.getId()).isEqualTo("ticketmaster:e0");
            assertThat(details.getTitle()).isEqualTo("Show 0");
            assertThat(details.getTimezone()).isEqualTo("America/New_York");
        }

        @Test
        @DisplayName("unknown id: NOT_FOUND / EVENT_NOT_FOUND with the id")
        void unknown() {
            when(eventQueryService.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById("ticketmaster:nope"))
                    .isInstanceOfSatisfying(ServiceValidationException.class, e -> {
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                        assertThat(e.getMessageCode()).isEqualTo(MessageCode.EVENT_NOT_FOUND);
                        assertThat(e.getArguments()).containsExactly("ticketmaster:nope");
                    });
        }

        @ParameterizedTest
        @ValueSource(strings = {"eventbrite:1", "TICKETMASTER:1", "ticketmaster:", "ticketmaster: ", ":1", "12345"})
        @DisplayName("unknown source or malformed id: NOT_FOUND, no lookup")
        void malformed(String id) {
            assertThatThrownBy(() -> service.getById(id))
                    .isInstanceOfSatisfying(ServiceValidationException.class, e -> {
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                        assertThat(e.getMessageCode()).isEqualTo(MessageCode.EVENT_NOT_FOUND);
                    });
            verify(eventQueryService, never()).findById(any());
        }
    }

    private static EventSearchRequest.EventSearchRequestBuilder request() {
        return EventSearchRequest.builder().city("New York");
    }

    private void stubEvents(int count) {
        when(eventQueryService.findEvents(any(), any(), any(), any()))
                .thenReturn(IntStream.range(0, count).mapToObj(EventViewServiceImplTest::event).toList());
    }

    private static void assertValidationError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call,
                                              String messageCode, String field) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ServiceValidationException.class, e -> {
            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
            assertThat(e.getMessageCode()).isEqualTo(messageCode);
            assertThat(e.getDetails()).containsEntry("field", field);
        });
    }

    private static Event event(int i) {
        return Event.builder()
                .id(new EventId(EventSource.TICKETMASTER, "e" + i))
                .title("Show " + i)
                .sourceUrl("https://www.ticketmaster.com/event/e" + i)
                .startAt(OffsetDateTime.parse("2026-11-20T20:00:00-05:00").plusMinutes(i))
                .timezone(NEW_YORK)
                .status(EventStatus.SCHEDULED)
                .category("Music")
                .interests(List.of(Interest.MUSIC))
                .build();
    }
}
