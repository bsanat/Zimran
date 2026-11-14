package kz.astyq.zimran.event.view.controller;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.i18n.impl.MessageServiceImpl;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.mapper.EventMapper;
import kz.astyq.zimran.event.model.domain.TestEvents;
import kz.astyq.zimran.event.model.dto.EventDetailsViewResponse;
import kz.astyq.zimran.event.model.dto.EventPageViewResponse;
import kz.astyq.zimran.event.model.dto.EventSearchRequest;
import kz.astyq.zimran.event.model.dto.PaginationViewResponse;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.event.util.MessageCode;
import kz.astyq.zimran.event.view.service.EventViewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventViewController.class)
@Import(MessageServiceImpl.class)
class EventViewControllerTest {

    private static final List<String> EVENT_KEYS = List.of("id", "source", "sourceEventId", "title", "category",
            "city", "startAt", "timezone", "venue", "address", "description", "imageUrl", "currency", "sourceUrl",
            "priceMin", "priceMax", "priceKnown", "status", "isMajor", "dateText", "priceText", "categorySlug", "isDemo");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventViewService eventViewService;

    private final EventMapper mapper = Mappers.getMapper(EventMapper.class);

    @Test
    @DisplayName("GET /api/events: events + pagination, every key present, lowercase enums, nulls kept")
    void page() throws Exception {
        when(eventViewService.getPage(any())).thenReturn(EventPageViewResponse.builder()
                .events(List.of(mapper.toViewResponse(TestEvents.event("e1")
                        .startAt(OffsetDateTime.parse("2026-11-20T20:00:00-05:00")).build())))
                .pagination(PaginationViewResponse.builder().page(1).pageSize(10).hasMore(true).build())
                .build());

        var result = mockMvc.perform(get("/api/events").param("city", "New York").param("category", "music"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.pageSize").value(10))
                .andExpect(jsonPath("$.pagination.hasMore").value(true))
                .andExpect(jsonPath("$.events[0].id").value("json:e1"))
                .andExpect(jsonPath("$.events[0].source").value("json"))
                .andExpect(jsonPath("$.events[0].category").value("music"))
                .andExpect(jsonPath("$.events[0].status").value("scheduled"))
                .andExpect(jsonPath("$.events[0].isMajor").value(false))
                .andExpect(jsonPath("$.events[0].isDemo").value(false))
                .andExpect(jsonPath("$.events[0].categorySlug").value("music"))
                .andExpect(jsonPath("$.events[0].startAt").value("2026-11-20T20:00:00-05:00"))
                .andExpect(jsonPath("$.events[0].priceMin").value(nullValue()))
                .andExpect(jsonPath("$.events[0].priceKnown").value(false));
        for (String key : EVENT_KEYS) {
            result.andExpect(jsonPath("$.events[0]", hasKey(key)));
        }
        ArgumentCaptor<EventSearchRequest> captor = ArgumentCaptor.forClass(EventSearchRequest.class);
        verify(eventViewService).getPage(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo(Interest.MUSIC);
    }

    @Test
    @DisplayName("GET /api/events: unknown category, page 0/101, long keyword, bad major, missing city -> 400 field")
    void invalidParams() throws Exception {
        mockMvc.perform(get("/api/events").param("city", "New York").param("category", "opera"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.field").value("category"));
        mockMvc.perform(get("/api/events").param("city", "New York").param("page", "101"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.field").value("page"));
        mockMvc.perform(get("/api/events").param("city", "New York").param("keyword", "k".repeat(101)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.field").value("keyword"));
        mockMvc.perform(get("/api/events").param("city", "New York").param("categorySlug", "opera"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.field").value("categorySlug"));
        mockMvc.perform(get("/api/events").param("city", "New York").param("budgetMax", "-1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.field").value("budgetMax"));
        mockMvc.perform(get("/api/events").param("city", "New York").param("freeOnly", "yes"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.field").value("freeOnly"));
        mockMvc.perform(get("/api/events").param("city", "New York").param("major", "yes"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.field").value("major"));
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.field").value("city"));
    }

    @Test
    @DisplayName("GET /api/events/{id}: URL-encoded id, {event: ...}; unknown -> 404 NOT_FOUND")
    void byId() throws Exception {
        when(eventViewService.getById("json:e1")).thenReturn(EventDetailsViewResponse.builder()
                .event(mapper.toViewResponse(TestEvents.event("e1").build())).build());
        when(eventViewService.getById("json:nope")).thenThrow(
                new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.EVENT_NOT_FOUND));

        mockMvc.perform(get(java.net.URI.create("/api/events/json%3Ae1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.event.id").value("json:e1"));
        mockMvc.perform(get(java.net.URI.create("/api/events/json%3Anope")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("This event could not be found."));
    }
}
