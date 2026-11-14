package kz.astyq.zimran.ai.util;

import kz.astyq.zimran.ai.model.domain.AiPairReason;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The JSON schema of the Pair Planning reason answer (strict JSON-schema structured output) and its server-side check:
 * <pre>
 * {"reasons": [{"eventId": string, "reasonA": string, "reasonB": string}]}
 * </pre>
 * The check never trusts the model: malformed items are dropped (the whole answer when the top level is wrong),
 * all text is sanitized and capped. Whether an {@code eventId} was sent is checked by the caller.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PairReasonResponseSchema {

    public static final String NAME = "pair_reasons";
    static final int REASON_MAX = 300;
    static final int MAX_ITEMS = 20;

    public static Map<String, Object> responseFormat() {
        Map<String, Object> item = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("eventId", "reasonA", "reasonB"),
                "properties", Map.of(
                        "eventId", Map.of("type", "string"),
                        "reasonA", Map.of("type", "string"),
                        "reasonB", Map.of("type", "string")));
        Map<String, Object> schema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("reasons"),
                "properties", Map.of("reasons", Map.of("type", "array", "items", item)));
        return Map.of("type", "json_schema",
                "json_schema", Map.of("name", NAME, "strict", true, "schema", schema));
    }

    /**
     * The sanitized reasons, or empty when {@code root} does not have the required top-level shape.
     */
    public static Optional<List<AiPairReason>> parse(JsonNode root) {
        if (root == null || !root.isObject()) {
            return Optional.empty();
        }
        JsonNode reasons = root.get("reasons");
        if (reasons == null || !reasons.isArray()) {
            return Optional.empty();
        }
        List<AiPairReason> result = new ArrayList<>();
        for (int i = 0; i < reasons.size() && result.size() < MAX_ITEMS; i++) {
            item(reasons.get(i)).ifPresent(result::add);
        }
        return Optional.of(List.copyOf(result));
    }

    private static Optional<AiPairReason> item(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Optional.empty();
        }
        JsonNode id = node.get("eventId");
        if (id == null || !id.isString() || id.stringValue().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(AiPairReason.builder()
                .eventId(id.stringValue().strip())
                .reasonA(text(node.get("reasonA")))
                .reasonB(text(node.get("reasonB")))
                .build());
    }

    private static String text(JsonNode node) {
        return node != null && node.isString() ? AiTextSanitizer.fromModel(node.stringValue(), REASON_MAX) : "";
    }
}
