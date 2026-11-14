package kz.astyq.zimran.ai.util;

import kz.astyq.zimran.ai.model.domain.AiPick;
import kz.astyq.zimran.ai.model.domain.AiRanking;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RankingResponseSchemaTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Nested
    @DisplayName("parse")
    class Parse {

        @Test
        @DisplayName("a valid answer, with text sanitized and scores clamped and rounded")
        void valid() {
            AiRanking ranking = parse("""
                    {"summary": "Great <b>night</b> out", "recommendations": [
                      {"eventId": "json:a", "matchScore": 120, "reason": "See https://evil.example now"},
                      {"eventId": "json:b", "matchScore": 42.6, "reason": "**Fun**"},
                      {"eventId": "json:c", "matchScore": -3, "reason": ""}],
                     "suggestions": ["Try Sunday", "", 7, "a", "b", "c"]}
                    """).orElseThrow();

            assertThat(ranking.getSummary()).isEqualTo("Great night out");
            assertThat(ranking.getPicks()).extracting(AiPick::getEventId).containsExactly("json:a", "json:b", "json:c");
            assertThat(ranking.getPicks()).extracting(AiPick::getMatchScore).containsExactly(100, 43, 0);
            assertThat(ranking.getPicks().get(0).getReason()).isEqualTo("See now");
            assertThat(ranking.getPicks().get(1).getReason()).isEqualTo("Fun");
            assertThat(ranking.getSuggestions()).containsExactly("Try Sunday", "a", "b");
        }

        @Test
        @DisplayName("malformed items are dropped, the rest kept")
        void malformedItems() {
            AiRanking ranking = parse("""
                    {"summary": "", "recommendations": [{"eventId": 5, "matchScore": 1}, "x",
                      {"eventId": "json:a", "matchScore": "high"}, {"eventId": " ", "matchScore": 1},
                      {"eventId": "json:b", "matchScore": 80}], "suggestions": []}
                    """).orElseThrow();

            assertThat(ranking.getPicks()).extracting(AiPick::getEventId).containsExactly("json:b");
            assertThat(ranking.getPicks().getFirst().getReason()).isEmpty();
        }

        @Test
        @DisplayName("wrong top-level shape: empty")
        void wrongShape() {
            assertThat(parse("[]")).isEmpty();
            assertThat(parse("{\"summary\": 1, \"recommendations\": []}")).isEmpty();
            assertThat(parse("{\"summary\": \"x\"}")).isEmpty();
        }
    }

    @Test
    @DisplayName("responseFormat: strict json_schema requiring summary, recommendations and suggestions")
    @SuppressWarnings("unchecked")
    void responseFormat() {
        Map<String, Object> format = RankingResponseSchema.responseFormat(true);
        Map<String, Object> jsonSchema = (Map<String, Object>) format.get("json_schema");
        Map<String, Object> schema = (Map<String, Object>) jsonSchema.get("schema");

        assertThat(format.get("type")).isEqualTo("json_schema");
        assertThat(jsonSchema.get("strict")).isEqualTo(true);
        assertThat((Iterable<String>) schema.get("required")).containsExactly("summary", "recommendations", "suggestions");
    }

    @Test
    @DisplayName("responseFormat without suggestions: only summary and picks (eventId, matchScore, reason) allowed")
    @SuppressWarnings("unchecked")
    void responseFormatWithoutSuggestions() {
        Map<String, Object> schema = (Map<String, Object>) ((Map<String, Object>) RankingResponseSchema
                .responseFormat(false).get("json_schema")).get("schema");

        assertThat((Iterable<String>) schema.get("required")).containsExactly("summary", "recommendations");
        assertThat((Map<String, Object>) schema.get("properties")).containsOnlyKeys("summary", "recommendations");
        assertThat(schema.get("additionalProperties")).isEqualTo(false);
        assertThat(parse("{\"summary\": \"x\", \"recommendations\": []}")).hasValueSatisfying(ranking ->
                assertThat(ranking.getSuggestions()).isEmpty());
    }

    private static Optional<AiRanking> parse(String json) {
        return RankingResponseSchema.parse(JSON.readTree(json));
    }
}
