package kz.astyq.zimran.event.view.controller;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.i18n.impl.MessageServiceImpl;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.model.dto.EventDetailsViewResponse;
import kz.astyq.zimran.event.model.dto.EventPageViewResponse;
import kz.astyq.zimran.event.model.dto.EventSearchRequest;
import kz.astyq.zimran.event.model.dto.EventViewResponse;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.event.util.MessageCode;
import kz.astyq.zimran.event.view.service.EventViewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventViewController.class)
@Import(MessageServiceImpl.class)
class EventViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventViewService eventViewService;

    @Nested
    @DisplayName("GET /api/events")
    class GetPage {

        @Test
        @DisplayName("200: page JSON with null optional fields, Cache-Control public 60 s")
        void ok() throws Exception {
            when(eventViewService.getPage(any())).thenReturn(EventPageViewResponse.builder()
                    .items(List.of(item()))
                    .page(1).pageSize(20).total(21).hasMore(true)
                    .build());

            mockMvc.perform(get("/api/events").param("city", "New York"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=60, public"))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.pageSize").value(20))
                    .andExpect(jsonPath("$.total").value(21))
                    .andExpect(jsonPath("$.hasMore").value(true))
                    .andExpect(jsonPath("$.items[0].id").value("ticketmaster:vvG1zZ9KbGkM1a"))
                    .andExpect(jsonPath("$.items[0].title").value("Jazz Night"))
                    .andExpect(jsonPath("$.items[0].category").value("Music"))
                    .andExpect(jsonPath("$.items[0].interests[0]").value("MUSIC"))
                    .andExpect(jsonPath("$.items[0].startAt").value("2026-11-20T20:00:00-05:00"))
                    .andExpect(jsonPath("$.items[0].timezone").value("America/New_York"))
                    .andExpect(jsonPath("$.items[0].status").value("SCHEDULED"))
                    .andExpect(jsonPath("$.items[0].priceKnown").value(false))
                    .andExpect(jsonPath("$.items[0].sourceUrl").value("https://www.ticketmaster.com/event/vvG1zZ9KbGkM1a"))
                    .andExpect(jsonPath("$.items[0]", hasKey("priceMin")))
                    .andExpect(jsonPath("$.items[0].priceMin").value(nullValue()))
                    .andExpect(jsonPath("$.items[0]", hasKey("priceMax")))
                    .andExpect(jsonPath("$.items[0]", hasKey("currency")))
                    .andExpect(jsonPath("$.items[0]", hasKey("venue")))
                    .andExpect(jsonPath("$.items[0].venue").value(nullValue()))
                    .andExpect(jsonPath("$.items[0]", hasKey("city")))
                    .andExpect(jsonPath("$.items[0]", hasKey("imageUrl")))
                    .andExpect(jsonPath("$.items[0].imageUrl").value(nullValue()));
        }

        @Test
        @DisplayName("query parameters are bound to EventSearchRequest")
        void binding() throws Exception {
            when(eventViewService.getPage(any())).thenReturn(EventPageViewResponse.builder().items(List.of()).build());

            mockMvc.perform(get("/api/events")
                            .param("city", "New York")
                            .param("startDate", "2026-11-20")
                            .param("endDate", "2026-11-22")
                            .param("category", "MUSIC")
                            .param("category", "COMEDY")
                            .param("page", "2")
                            .param("pageSize", "50"))
                    .andExpect(status().isOk());

            ArgumentCaptor<EventSearchRequest> captor = ArgumentCaptor.forClass(EventSearchRequest.class);
            verify(eventViewService).getPage(captor.capture());
            EventSearchRequest request = captor.getValue();
            assertThat(request.getCity()).isEqualTo("New York");
            assertThat(request.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 20));
            assertThat(request.getEndDate()).isEqualTo(LocalDate.of(2026, 11, 22));
            assertThat(request.getDate()).isNull();
            assertThat(request.getCategory()).containsExactly(Interest.MUSIC, Interest.COMEDY);
            assertThat(request.getPage()).isEqualTo(2);
            assertThat(request.getPageSize()).isEqualTo(50);
        }

        @Test
        @DisplayName("comma-separated category is split")
        void commaSeparatedCategory() throws Exception {
            when(eventViewService.getPage(any())).thenReturn(EventPageViewResponse.builder().items(List.of()).build());

            mockMvc.perform(get("/api/events").param("city", "New York").param("category", "MUSIC,ART"))
                    .andExpect(status().isOk());

            ArgumentCaptor<EventSearchRequest> captor = ArgumentCaptor.forClass(EventSearchRequest.class);
            verify(eventViewService).getPage(captor.capture());
            assertThat(captor.getValue().getCategory()).containsExactly(Interest.MUSIC, Interest.ART);
        }

        @Test
        @DisplayName("400: pageSize 51")
        void pageSizeTooLarge() throws Exception {
            mockMvc.perform(get("/api/events").param("city", "New York").param("pageSize", "51"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.error.details.field").value("pageSize"))
                    .andExpect(header().doesNotExist(HttpHeaders.CACHE_CONTROL));
            verifyNoInteractions(eventViewService);
        }

        @Test
        @DisplayName("400: unknown category, without echoing Java internals")
        void unknownCategory() throws Exception {
            mockMvc.perform(get("/api/events").param("city", "New York").param("category", "BALLOONING"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.error.details.field").value("category"))
                    .andExpect(jsonPath("$.error.details.fields[0].message").value("Invalid value"));
            verifyNoInteractions(eventViewService);
        }

        @Test
        @DisplayName("400: missing city, malformed date")
        void missingCityAndBadDate() throws Exception {
            mockMvc.perform(get("/api/events"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details.field").value("city"));
            mockMvc.perform(get("/api/events").param("city", "New York").param("date", "2026-13-01"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details.field").value("date"));
            verifyNoInteractions(eventViewService);
        }

        @Test
        @DisplayName("400: service date validation error keeps its field detail")
        void serviceValidation() throws Exception {
            when(eventViewService.getPage(any())).thenThrow(new ServiceValidationException(
                    ErrorCode.VALIDATION_ERROR, MessageCode.DATE_RANGE_TOO_LONG, 7).withDetail("field", "endDate"));

            mockMvc.perform(get("/api/events").param("city", "New York"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.error.message").value("The date range must not exceed 7 days"))
                    .andExpect(jsonPath("$.error.details.field").value("endDate"));
        }

        @Test
        @DisplayName("503: provider unavailable")
        void upstreamUnavailable() throws Exception {
            when(eventViewService.getPage(any())).thenThrow(new ServiceValidationException(
                    ErrorCode.UPSTREAM_UNAVAILABLE, kz.astyq.zimran.ticketmaster.util.MessageCode.TICKETMASTER_UNAVAILABLE));

            mockMvc.perform(get("/api/events").param("city", "New York"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error.code").value("UPSTREAM_UNAVAILABLE"))
                    .andExpect(jsonPath("$.error.message").isNotEmpty())
                    .andExpect(jsonPath("$.error.requestId").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/events/{id}")
    class GetById {

        @Test
        @DisplayName("200: URL-encoded id is decoded; details JSON; Cache-Control")
        void ok() throws Exception {
            when(eventViewService.getById("ticketmaster:12345")).thenReturn(EventDetailsViewResponse.builder()
                    .id("ticketmaster:12345")
                    .title("Jazz Night")
                    .description(null)
                    .address("178 7th Ave S, New York, NY, 10014")
                    .startAt(OffsetDateTime.parse("2026-11-20T20:00:00-05:00"))
                    .timezone("America/New_York")
                    .status(EventStatus.POSTPONED)
                    .priceMin(new BigDecimal("35.00"))
                    .priceMax(new BigDecimal("89.50"))
                    .currency("USD")
                    .priceKnown(true)
                    .interests(List.of())
                    .build());

            mockMvc.perform(get(URI.create("/api/events/ticketmaster%3A12345")))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=60, public"))
                    .andExpect(jsonPath("$.id").value("ticketmaster:12345"))
                    .andExpect(jsonPath("$.status").value("POSTPONED"))
                    .andExpect(jsonPath("$.address").value("178 7th Ave S, New York, NY, 10014"))
                    .andExpect(jsonPath("$.priceMin").value(35.00))
                    .andExpect(jsonPath("$.priceKnown").value(true))
                    .andExpect(jsonPath("$", hasKey("description")))
                    .andExpect(jsonPath("$.description").value(nullValue()));
        }

        @Test
        @DisplayName("404: error envelope")
        void notFound() throws Exception {
            when(eventViewService.getById("ticketmaster:nope")).thenThrow(new ServiceValidationException(
                    ErrorCode.NOT_FOUND, MessageCode.EVENT_NOT_FOUND, "ticketmaster:nope"));

            mockMvc.perform(get(URI.create("/api/events/ticketmaster%3Anope")).header("X-Request-Id", "client-req-12345"))
                    .andExpect(status().isNotFound())
                    .andExpect(header().doesNotExist(HttpHeaders.CACHE_CONTROL))
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.error.message").value("Event ticketmaster:nope was not found"))
                    .andExpect(jsonPath("$.error.details").isMap())
                    .andExpect(jsonPath("$.error.requestId").value("client-req-12345"));
        }

        @Test
        @DisplayName("503: provider unavailable")
        void upstreamUnavailable() throws Exception {
            when(eventViewService.getById(any())).thenThrow(new ServiceValidationException(
                    ErrorCode.UPSTREAM_UNAVAILABLE, kz.astyq.zimran.ticketmaster.util.MessageCode.TICKETMASTER_UNAVAILABLE));

            mockMvc.perform(get("/api/events/{id}", "ticketmaster:1"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error.code").value("UPSTREAM_UNAVAILABLE"));
        }
    }

    private static EventViewResponse item() {
        return EventViewResponse.builder()
                .id("ticketmaster:vvG1zZ9KbGkM1a")
                .title("Jazz Night")
                .category("Music")
                .interests(List.of(Interest.MUSIC))
                .startAt(OffsetDateTime.parse("2026-11-20T20:00:00-05:00"))
                .timezone("America/New_York")
                .status(EventStatus.SCHEDULED)
                .priceKnown(false)
                .sourceUrl("https://www.ticketmaster.com/event/vvG1zZ9KbGkM1a")
                .build();
    }
}
