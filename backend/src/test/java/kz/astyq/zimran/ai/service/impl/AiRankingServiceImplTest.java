package kz.astyq.zimran.ai.service.impl;

import kz.astyq.zimran.ai.model.domain.AiCandidate;
import kz.astyq.zimran.ai.model.domain.AiRanking;
import kz.astyq.zimran.ai.model.domain.AiRankingRequest;
import kz.astyq.zimran.ai.service.AiCompletionService;
import kz.astyq.zimran.ai.util.MessageCode;
import kz.astyq.zimran.core.configuration.GeminiProperties;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRankingServiceImplTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Mock
    private AiCompletionService aiCompletionService;

    private final GeminiProperties properties = new GeminiProperties();
    private AiRankingServiceImpl service;

    @BeforeEach
    void setUp() {
        properties.setApiKey("test-key");
        service = new AiRankingServiceImpl(aiCompletionService, properties, JSON);
    }

    @Nested
    @DisplayName("rank")
    class Rank {

        @Test
        @DisplayName("a schema-valid answer is parsed; the schema has suggestions only when asked for")
        @SuppressWarnings("unchecked")
        void ok() {
            when(aiCompletionService.complete(anyList(), any())).thenReturn(JSON.readTree(
                    "{\"summary\":\"Nice\",\"recommendations\":[{\"eventId\":\"json:a\",\"matchScore\":90,\"reason\":\"Jazz\"}]}"));

            Optional<AiRanking> ranking = service.rank(request(false));

            assertThat(ranking).isPresent();
            assertThat(ranking.get().getPicks()).singleElement().satisfies(pick -> {
                assertThat(pick.getEventId()).isEqualTo("json:a");
                assertThat(pick.getMatchScore()).isEqualTo(90);
            });
            ArgumentCaptor<Map<String, Object>> format = ArgumentCaptor.forClass(Map.class);
            verify(aiCompletionService).complete(anyList(), format.capture());
            Map<String, Object> schema = (Map<String, Object>) ((Map<String, Object>) format.getValue()
                    .get("json_schema")).get("schema");
            assertThat((Map<String, Object>) schema.get("properties")).doesNotContainKey("suggestions");
        }

        @Test
        @DisplayName("no API key, or no candidates: empty without calling the model")
        void notCalled() {
            properties.setApiKey(" ");
            assertThat(service.rank(request(true))).isEmpty();
            properties.setApiKey("k");
            assertThat(service.rank(AiRankingRequest.builder().language("en").city("New York").interests(List.of("music"))
                    .startDate("d").endDate("d").timePreference("any").candidates(List.of()).build())).isEmpty();
            verifyNoInteractions(aiCompletionService);
        }

        @Test
        @DisplayName("AI unavailable or off-schema answer: empty, never throws")
        void failures() {
            when(aiCompletionService.complete(anyList(), any()))
                    .thenThrow(new ServiceValidationException(ErrorCode.AI_UNAVAILABLE, MessageCode.AI_UNAVAILABLE))
                    .thenReturn(JSON.readTree("{\"answer\": 1}"));

            assertThat(service.rank(request(true))).isEmpty();
            assertThat(service.rank(request(true))).isEmpty();
        }
    }

    private static AiRankingRequest request(boolean withSuggestions) {
        return AiRankingRequest.builder()
                .language("en").city("New York").interests(List.of("music"))
                .startDate("2026-11-20").endDate("2026-11-20").timePreference("any")
                .candidates(List.of(AiCandidate.builder().id("json:a").title("Jazz").category("music")
                        .interests(List.of("music")).build()))
                .maxPicks(5)
                .withSuggestions(withSuggestions)
                .build();
    }
}
