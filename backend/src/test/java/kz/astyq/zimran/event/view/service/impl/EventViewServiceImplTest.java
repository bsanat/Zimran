package kz.astyq.zimran.event.view.service.impl;

import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.mapper.EventMapper;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventCriteria;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.domain.TestEvents;
import kz.astyq.zimran.event.model.dto.EventPageViewResponse;
import kz.astyq.zimran.event.model.dto.EventSearchRequest;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.event.service.EventQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static kz.astyq.zimran.event.model.domain.TestEvents.NEW_YORK;
import static kz.astyq.zimran.event.model.domain.TestEvents.NYC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventViewServiceImplTest {

    /** 2026-11-14T23:30 in New York = 2026-11-15T04:30Z. */
    private static final Clock CLOCK = Clock.fixed(
            LocalDateTime.of(2026, 11, 14, 23, 30).atZone(NEW_YORK).toInstant(), NEW_YORK);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);

    @Mock
    private EventQueryService eventQueryService;
    @Mock
    private SupportedCityService supportedCityService;

    private EventViewServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(supportedCityService.getByName(eq("New York"), any())).thenReturn(NYC);
        service = new EventViewServiceImpl(eventQueryService, supportedCityService,
                Mappers.getMapper(EventMapper.class), CLOCK);
    }

    @Nested
    @DisplayName("getPage")
    class GetPage {

        @Test
        @DisplayName("no date: from today (New York) on, undated included; filters are passed on")
        void criteria() {
            when(eventQueryService.search(any())).thenReturn(List.of());

            service.getPage(EventSearchRequest.builder().city("New York").state("ny").category(Interest.FOOD)
                    .keyword("jazz").major("true").build());

            ArgumentCaptor<EventCriteria> captor = ArgumentCaptor.forClass(EventCriteria.class);
            verify(eventQueryService).search(captor.capture());
            EventCriteria criteria = captor.getValue();
            assertThat(criteria.getFromDate()).isEqualTo(TODAY);
            assertThat(criteria.getToDate()).isNull();
            assertThat(criteria.isIncludeUndated()).isTrue();
            assertThat(criteria.getInterests()).containsExactly(Interest.FOOD);
            assertThat(criteria.getKeyword()).isEqualTo("jazz");
            assertThat(criteria.isMajorOnly()).isTrue();
        }

        @Test
        @DisplayName("a date: only that day, no undated events")
        void singleDay() {
            when(eventQueryService.search(any())).thenReturn(List.of());

            service.getPage(EventSearchRequest.builder().city("New York").date(TODAY).build());

            ArgumentCaptor<EventCriteria> captor = ArgumentCaptor.forClass(EventCriteria.class);
            verify(eventQueryService).search(captor.capture());
            assertThat(captor.getValue().getFromDate()).isEqualTo(TODAY);
            assertThat(captor.getValue().getToDate()).isEqualTo(TODAY);
            assertThat(captor.getValue().isIncludeUndated()).isFalse();
        }

        @Test
        @DisplayName("a date before today in New York: VALIDATION_ERROR field=date")
        void pastDate() {
            assertInvalid(EventSearchRequest.builder().city("New York").date(TODAY.minusDays(1)).build(), "date");
        }

        @Test
        @DisplayName("a state other than the city's: the city service's VALIDATION_ERROR field=state")
        void wrongState() {
            when(supportedCityService.getByName("New York", "CA")).thenThrow(new ServiceValidationException(
                    ErrorCode.VALIDATION_ERROR, "state_not_supported").withDetail("field", "state"));
            assertInvalid(EventSearchRequest.builder().city("New York").state("CA").build(), "state");
        }

        @Test
        @DisplayName("pages of 10; hasMore while more events follow")
        void pagination() {
            List<Event> events = IntStream.range(0, 21).mapToObj(i -> TestEvents.event("e" + i).build()).toList();
            when(eventQueryService.search(any())).thenReturn(events);

            EventPageViewResponse first = service.getPage(EventSearchRequest.builder().city("New York").build());
            EventPageViewResponse third = service.getPage(EventSearchRequest.builder().city("New York").page(3).build());
            EventPageViewResponse beyond = service.getPage(EventSearchRequest.builder().city("New York").page(9).build());

            assertThat(first.getEvents()).hasSize(10);
            assertThat(first.getPagination().getPage()).isEqualTo(1);
            assertThat(first.getPagination().getPageSize()).isEqualTo(10);
            assertThat(first.getPagination().isHasMore()).isTrue();
            assertThat(third.getEvents()).extracting("id").containsExactly("json:e20");
            assertThat(third.getPagination().isHasMore()).isFalse();
            assertThat(beyond.getEvents()).isEmpty();
            assertThat(beyond.getPagination().isHasMore()).isFalse();
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("known id: {event: ...}")
        void found() {
            when(eventQueryService.findById(new EventId(EventSource.JSON, "e1")))
                    .thenReturn(Optional.of(TestEvents.event("e1").build()));

            assertThat(service.getById("json:e1").getEvent().getId()).isEqualTo("json:e1");
        }

        @Test
        @DisplayName("unknown or malformed id: NOT_FOUND")
        void notFound() {
            when(eventQueryService.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById("json:nope"))
                    .isInstanceOfSatisfying(ServiceValidationException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
            assertThatThrownBy(() -> service.getById("garbage"))
                    .isInstanceOfSatisfying(ServiceValidationException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    private void assertInvalid(EventSearchRequest request, String field) {
        assertThatThrownBy(() -> service.getPage(request))
                .isInstanceOfSatisfying(ServiceValidationException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(e.getDetails()).containsEntry("field", field);
                });
    }
}
