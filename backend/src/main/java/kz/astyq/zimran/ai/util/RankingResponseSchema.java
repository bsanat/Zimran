package kz.astyq.zimran.ai.util;

import kz.astyq.zimran.ai.model.domain.AiPick;
import kz.astyq.zimran.ai.model.domain.AiRanking;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The JSON schema the model must answer with (OpenAI strict structured output), and its server-side check:
 * <pre>
 * {"summary": string, "recommendations": [{"eventId": string, "matchScore": integer, "reason": string}],
 *  "suggestions": [string]}
 * </pre>
 * The check never trusts the model: wrong types drop the item (or the whole answer when the top level is
 * wrong), scores are clamped to 0..100, all text is sanitized and capped.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RankingResponseSchema {

    public static final String NAME = "event_ranking";
    static final int SUMMARY_MAX = 400;
    static final int REASON_MAX = 300;
    static final int SUGGESTION_MAX = 200;
    static final int MAX_SUGGESTIONS = 3;
    static final int MAX_PICKS = 20;

    /**
     * {@code response_format} value for the chat completions request.
     */
    public static Map<String, Object> responseFormat() {
        Map<String, Object> pick = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("eventId", "matchScore", "reason"),
                "properties", Map.of(
                        "eventId", Map.of("type", "string"),
                        "matchScore", Map.of("type", "integer"),
                        "reason", Map.of("type", "string")));
        Map<String, Object> schema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("summary", "recommendations", "suggestions"),
                "properties", Map.of(
                        "summary", Map.of("type", "string"),
                        "recommendations", Map.of("type", "array", "items", pick),
                        "suggestions", Map.of("type", "array", "items", Map.of("type", "string"))));
        return Map.of("type", "json_schema",
                "json_schema", Map.of("name", NAME, "strict", true, "schema", schema));
    }

    /**
     * The sanitized answer, or empty when {@code root} does not have the required top-level shape.
     */
    public static Optional<AiRanking> parse(JsonNode root) {
        if (root == null || !root.isObject()) {
            return Optional.empty();
        }
        JsonNode summary = root.get("summary");
        JsonNode recommendations = root.get("recommendations");
        if (summary == null || !summary.isString() || recommendations == null || !recommendations.isArray()) {
            return Optional.empty();
        }
        List<AiPick> picks = new ArrayList<>();
        for (int i = 0; i < recommendations.size() && picks.size() < MAX_PICKS; i++) {
            pick(recommendations.get(i)).ifPresent(picks::add);
        }
        List<String> suggestions = new ArrayList<>();
        JsonNode suggestionNodes = root.get("suggestions");
        if (suggestionNodes != null && suggestionNodes.isArray()) {
            for (int i = 0; i < suggestionNodes.size() && suggestions.size() < MAX_SUGGESTIONS; i++) {
                JsonNode node = suggestionNodes.get(i);
                String text = node != null && node.isString()
                        ? AiTextSanitizer.fromModel(node.stringValue(), SUGGESTION_MAX) : "";
                if (!text.isEmpty()) {
                    suggestions.add(text);
                }
            }
        }
        return Optional.of(AiRanking.builder()
                .summary(AiTextSanitizer.fromModel(summary.stringValue(), SUMMARY_MAX))
                .picks(List.copyOf(picks))
                .suggestions(List.copyOf(suggestions))
                .build());
    }

    private static Optional<AiPick> pick(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Optional.empty();
        }
        JsonNode id = node.get("eventId");
        JsonNode score = node.get("matchScore");
        JsonNode reason = node.get("reason");
        if (id == null || !id.isString() || id.stringValue().isBlank() || score == null || !score.isNumber()) {
            return Optional.empty();
        }
        double value = score.numberValue().doubleValue();
        if (!Double.isFinite(value)) {
            return Optional.empty();
        }
        int clamped = (int) Math.max(0, Math.min(100, Math.round(value)));
        String text = reason != null && reason.isString() ? AiTextSanitizer.fromModel(reason.stringValue(), REASON_MAX) : "";
        return Optional.of(AiPick.builder().eventId(id.stringValue().strip()).matchScore(clamped).reason(text).build());
    }
}
