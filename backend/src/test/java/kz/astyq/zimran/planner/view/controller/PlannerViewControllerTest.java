package kz.astyq.zimran.planner.view.controller;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.i18n.impl.MessageServiceImpl;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.mapper.EventMapper;
import kz.astyq.zimran.event.model.domain.TestEvents;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.planner.model.dto.PlanCreateRequest;
import kz.astyq.zimran.planner.model.dto.PlanViewResponse;
import kz.astyq.zimran.planner.model.dto.RecommendationViewResponse;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import kz.astyq.zimran.planner.util.MessageCode;
import kz.astyq.zimran.planner.view.service.PlannerViewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlannerViewController.class)
@Import(MessageServiceImpl.class)
class PlannerViewControllerTest {

    private static final String BODY = """
            {"city":"New York","interests":["music","food"],"startDate":"2026-11-20","endDate":"2026-11-21",
             "budgetMax":25,"timePreference":"evening","notes":"no loud venues"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlannerViewService plannerViewService;

    @Test
    @DisplayName("200: lowercase body is read; response has planId, summary, recommendations[event, matchScore, reason], suggestions")
    void ok() throws Exception {
        EventMapper eventMapper = Mappers.getMapper(EventMapper.class);
        when(plannerViewService.createPlan(any())).thenReturn(PlanViewResponse.builder()
                .planId("3f2b8c1e-1d1a-4f7e-9c55-0b5c2a7e9d10")
                .summary("Two events")
                .recommendations(List.of(RecommendationViewResponse.builder()
                        .event(eventMapper.toViewResponse(TestEvents.event("e1").build()))
                        .matchScore(87).reason("Jazz").build()))
                .suggestions(List.of())
                .build());

        mockMvc.perform(post("/api/plan").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value("3f2b8c1e-1d1a-4f7e-9c55-0b5c2a7e9d10"))
                .andExpect(jsonPath("$.summary").value("Two events"))
                .andExpect(jsonPath("$.recommendations[0].event.id").value("json:e1"))
                .andExpect(jsonPath("$.recommendations[0].event.status").value("scheduled"))
                .andExpect(jsonPath("$.recommendations[0].event.category").value("music"))
                .andExpect(jsonPath("$.recommendations[0].matchScore").value(87))
                .andExpect(jsonPath("$.recommendations[0].reason").value("Jazz"))
                .andExpect(jsonPath("$.suggestions").isArray());

        ArgumentCaptor<PlanCreateRequest> captor = ArgumentCaptor.forClass(PlanCreateRequest.class);
        verify(plannerViewService).createPlan(captor.capture());
        PlanCreateRequest request = captor.getValue();
        assertThat(request.getInterests()).containsExactly(Interest.MUSIC, Interest.FOOD);
        assertThat(request.getTimePreference()).isEqualTo(TimePreference.EVENING);
        assertThat(request.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(request.getBudgetMax()).isEqualByComparingTo(new BigDecimal("25"));
    }

    @Test
    @DisplayName("400 with details.field for an unknown interest, time preference or bad date")
    void unreadableFields() throws Exception {
        mockMvc.perform(post("/api/plan").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("\"food\"", "\"opera\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.field").value("interests"));
        mockMvc.perform(post("/api/plan").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("\"evening\"", "\"night\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.field").value("timePreference"));
        mockMvc.perform(post("/api/plan").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("2026-11-20", "2026-13-45")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.field").value("startDate"));
    }

    @Test
    @DisplayName("400: business validation errors keep their field and message")
    void businessValidation() throws Exception {
        when(plannerViewService.createPlan(any())).thenThrow(new ServiceValidationException(
                ErrorCode.VALIDATION_ERROR, MessageCode.INTERESTS_REQUIRED).withDetail("field", "interests"));

        mockMvc.perform(post("/api/plan").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.field").value("interests"))
                .andExpect(jsonPath("$.error.message").value("Choose at least one interest."));
    }

    @Test
    @DisplayName("413 for a body over 10 KB")
    void tooLarge() throws Exception {
        String big = BODY.replace("no loud venues", "x".repeat(11_000));

        mockMvc.perform(post("/api/plan").contentType(MediaType.APPLICATION_JSON).content(big))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error.code").value("PAYLOAD_TOO_LARGE"));
    }
}
