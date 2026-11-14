package kz.astyq.zimran.planner.service.impl;

import kz.astyq.zimran.ai.model.domain.AiPick;
import kz.astyq.zimran.ai.model.domain.AiRanking;
import kz.astyq.zimran.ai.model.domain.AiRankingRequest;
import kz.astyq.zimran.ai.service.AiRankingService;
import kz.astyq.zimran.core.configuration.PlannerProperties;
import kz.astyq.zimran.event.model.domain.TestEvents;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.planner.model.domain.Candidate;
import kz.astyq.zimran.planner.model.domain.CandidateSet;
import kz.astyq.zimran.planner.model.domain.FilterStats;
import kz.astyq.zimran.planner.model.domain.Plan;
import kz.astyq.zimran.planner.model.domain.Recommendation;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.model.enums.BudgetFit;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import kz.astyq.zimran.planner.service.FallbackRankingService;
import kz.astyq.zimran.planner.service.PlanExplanationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static kz.astyq.zimran.event.model.domain.TestEvents.NYC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 20);

    @Mock
    private AiRankingService aiRankingService;
    @Mock
    private FallbackRankingService fallbackRankingService;
    @Mock
    private PlanExplanationService planExplanationService;

    private RecommendationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RecommendationServiceImpl(aiRankingService, fallbackRankingService, planExplanationService,
                new PlannerProperties());
        lenient().when(planExplanationService.reasonFor(any(), any())).thenReturn("rule reason");
        lenient().when(planExplanationService.summaryFor(any(Integer.class), any())).thenReturn("rule summary");
        lenient().when(planExplanationService.notesNotUsed()).thenReturn("notes not used");
    }

    @Nested
    @DisplayName("recommend")
    class Recommend {

        @Test
        @DisplayName("AI picks: unknown and duplicate ids dropped, model order kept, events come from candidates")
        void aiPicksFiltered() {
            CandidateSet set = candidates(3);
            when(aiRankingService.rank(any())).thenReturn(Optional.of(ranking(
                    pick("json:c2", 95, "Great"), pick("json:invented", 99, "Fake"),
                    pick("json:c2", 50, "Again"), pick("json:c0", 70, ""))));

            Plan plan = service.recommend(request(null), set, "ru");

            assertThat(plan.isAiRanked()).isTrue();
            assertThat(plan.getRecommendations()).extracting(r -> r.getEvent().getId().asString())
                    .containsExactly("json:c2", "json:c0");
            assertThat(plan.getRecommendations()).extracting(Recommendation::getMatchScore).containsExactly(95, 70);
            assertThat(plan.getRecommendations()).extracting(Recommendation::getReason)
                    .containsExactly("Great", "rule reason");
            assertThat(plan.getRecommendations().getFirst().getEvent()).isSameAs(set.getCandidates().get(2).getEvent());
            assertThat(plan.getSummary()).isEqualTo("AI summary");
            assertThat(plan.getSuggestions()).containsExactly("AI tip");
            ArgumentCaptor<AiRankingRequest> captor = ArgumentCaptor.forClass(AiRankingRequest.class);
            verify(aiRankingService).rank(captor.capture());
            assertThat(captor.getValue().getLanguage()).isEqualTo("ru");
            assertThat(captor.getValue().getMaxPicks()).isEqualTo(5);
        }

        @Test
        @DisplayName("at most 5 AI picks are kept")
        void maxFive() {
            CandidateSet set = candidates(8);
            AiPick[] picks = IntStream.range(0, 8).mapToObj(i -> pick("json:c" + i, 80, "r")).toArray(AiPick[]::new);
            when(aiRankingService.rank(any())).thenReturn(Optional.of(ranking(picks)));

            assertThat(service.recommend(request(null), set, "en").getRecommendations()).hasSize(5);
        }

        @Test
        @DisplayName("AI unavailable: the rule-based ranking answers, with hints and the notes notice")
        void fallback() {
            CandidateSet set = candidates(2);
            List<Recommendation> fallback = List.of(Recommendation.builder()
                    .event(set.getCandidates().getFirst().getEvent()).matchScore(60).reason("rule reason").build());
            when(aiRankingService.rank(any())).thenReturn(Optional.empty());
            when(fallbackRankingService.rank(set, request("quiet please"))).thenReturn(fallback);

            Plan plan = service.recommend(request("quiet please"), set, "en");

            assertThat(plan.isAiRanked()).isFalse();
            assertThat(plan.getRecommendations()).isEqualTo(fallback);
            assertThat(plan.getSummary()).isEqualTo("rule summary");
            assertThat(plan.getSuggestions()).containsExactly("hint", "notes not used");
        }

        @Test
        @DisplayName("AI named only unknown events: the rule-based ranking answers")
        void allPicksInvalid() {
            CandidateSet set = candidates(2);
            when(aiRankingService.rank(any())).thenReturn(Optional.of(ranking(pick("json:nope", 90, "x"))));
            when(fallbackRankingService.rank(any(), any())).thenReturn(List.of());

            assertThat(service.recommend(request(null), set, "en").isAiRanked()).isFalse();
        }

        @Test
        @DisplayName("a valid AI answer with no picks is kept (nothing fits)")
        void aiEmpty() {
            when(aiRankingService.rank(any())).thenReturn(Optional.of(ranking()));

            Plan plan = service.recommend(request(null), candidates(2), "en");

            assertThat(plan.isAiRanked()).isTrue();
            assertThat(plan.getRecommendations()).isEmpty();
            verifyNoInteractions(fallbackRankingService);
        }

        @Test
        @DisplayName("no candidates: AI is not asked")
        void noCandidates() {
            when(fallbackRankingService.rank(any(), any())).thenReturn(List.of());

            Plan plan = service.recommend(request(null), candidates(0), "en");

            assertThat(plan.getRecommendations()).isEmpty();
            assertThat(plan.getId()).isNotNull();
            verifyNoInteractions(aiRankingService);
        }
    }

    private static ValidatedPlanRequest request(String notes) {
        return ValidatedPlanRequest.builder().city(NYC).interests(Set.of(Interest.MUSIC)).startDate(DAY).endDate(DAY)
                .timePreference(TimePreference.ANY).notes(notes).build();
    }

    private static CandidateSet candidates(int count) {
        List<Candidate> candidates = IntStream.range(0, count).mapToObj(i -> Candidate.builder()
                .event(TestEvents.eventAt("c" + i, DAY, "19:00").build())
                .matchedInterests(Set.of(Interest.MUSIC))
                .budgetFit(BudgetFit.UNKNOWN)
                .preScore(50)
                .build()).toList();
        return CandidateSet.builder().candidates(candidates).stats(FilterStats.builder().build())
                .hints(List.of("hint")).build();
    }

    private static AiRanking ranking(AiPick... picks) {
        return AiRanking.builder().summary("AI summary").picks(List.of(picks)).suggestions(List.of("AI tip")).build();
    }

    private static AiPick pick(String id, int score, String reason) {
        return AiPick.builder().eventId(id).matchScore(score).reason(reason).build();
    }
}
