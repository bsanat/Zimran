package kz.astyq.zimran.ai.service.impl;

import kz.astyq.zimran.ai.client.OpenAiClient;
import kz.astyq.zimran.ai.model.domain.AiCandidate;
import kz.astyq.zimran.ai.model.domain.AiRanking;
import kz.astyq.zimran.ai.model.domain.AiRankingRequest;
import kz.astyq.zimran.ai.model.dto.ChatCompletionRequest;
import kz.astyq.zimran.ai.service.AiUsageGuard;
import kz.astyq.zimran.ai.util.MessageCode;
import kz.astyq.zimran.core.configuration.OpenAiProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRankingServiceImplTest {

    @Mock
    private OpenAiClient openAiClient;
    @Mock
    private AiUsageGuard aiUsageGuard;

    private final OpenAiProperties properties = new OpenAiProperties();
    private AiRankingServiceImpl service;

    @BeforeEach
    void setUp() {
        properties.setApiKey("test-key");
        properties.setTotalTimeout(Duration.ofMillis(300));
        properties.setReadTimeout(Duration.ofMillis(300));
        service = new AiRankingServiceImpl(openAiClient, aiUsageGuard, properties, JsonMapper.builder().build());
    }

    @Nested
    @DisplayName("rank")
    class Rank {

        @Test
        @DisplayName("a schema-valid answer is parsed; the request uses the model, schema and token limit")
        void ok() {
            when(aiUsageGuard.tryAcquire()).thenReturn(true);
            when(openAiClient.complete(any())).thenReturn(
                    "{\"summary\":\"Nice\",\"recommendations\":[{\"eventId\":\"json:a\",\"matchScore\":90,\"reason\":\"Jazz\"}],\"suggestions\":[]}");

            Optional<AiRanking> ranking = service.rank(request());

            assertThat(ranking).isPresent();
            assertThat(ranking.get().getPicks()).singleElement().satisfies(pick -> {
                assertThat(pick.getEventId()).isEqualTo("json:a");
                assertThat(pick.getMatchScore()).isEqualTo(90);
            });
            ArgumentCaptor<ChatCompletionRequest> captor = ArgumentCaptor.forClass(ChatCompletionRequest.class);
            verify(openAiClient).complete(captor.capture());
            assertThat(captor.getValue().getModel()).isEqualTo("gpt-5-mini");
            assertThat(captor.getValue().getResponseFormat()).containsEntry("type", "json_schema");
            assertThat(captor.getValue().getMaxCompletionTokens()).isEqualTo(2000);
        }

        @Test
        @DisplayName("no API key, or no candidates: empty without calling OpenAI")
        void notCalled() {
            properties.setApiKey(" ");
            assertThat(service.rank(request())).isEmpty();
            properties.setApiKey("k");
            assertThat(service.rank(AiRankingRequest.builder().language("en").city("New York").interests(List.of("music"))
                    .startDate("d").endDate("d").timePreference("any").candidates(List.of()).build())).isEmpty();
            verifyNoInteractions(openAiClient, aiUsageGuard);
        }

        @Test
        @DisplayName("usage cap reached: empty")
        void capped() {
            when(aiUsageGuard.tryAcquire()).thenReturn(false);

            assertThat(service.rank(request())).isEmpty();
            verifyNoInteractions(openAiClient);
        }

        @Test
        @DisplayName("client error, non-JSON or off-schema answer: empty")
        void failures() {
            when(aiUsageGuard.tryAcquire()).thenReturn(true);
            when(openAiClient.complete(any()))
                    .thenThrow(new ServiceValidationException(ErrorCode.AI_UNAVAILABLE, MessageCode.AI_UNAVAILABLE))
                    .thenReturn("not json")
                    .thenReturn("{\"answer\": 1}");

            assertThat(service.rank(request())).isEmpty();
            assertThat(service.rank(request())).isEmpty();
            assertThat(service.rank(request())).isEmpty();
        }

        @Test
        @DisplayName("slower than the total timeout: empty in about the timeout")
        void timeout() {
            when(aiUsageGuard.tryAcquire()).thenReturn(true);
            when(openAiClient.complete(any())).thenAnswer(invocation -> {
                Thread.sleep(5_000);
                return "{}";
            });

            long start = System.nanoTime();
            assertThat(service.rank(request())).isEmpty();
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
        }
    }

    private static AiRankingRequest request() {
        return AiRankingRequest.builder()
                .language("en").city("New York").interests(List.of("music"))
                .startDate("2026-11-20").endDate("2026-11-20").timePreference("any")
                .candidates(List.of(AiCandidate.builder().id("json:a").title("Jazz").category("music")
                        .interests(List.of("music")).build()))
                .maxPicks(5)
                .build();
    }
}
