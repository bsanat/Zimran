package kz.astyq.zimran.ai.util;

import kz.astyq.zimran.ai.model.domain.AiExtraction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The JSON schema of the filter-extraction answer (strict JSON-schema structured output), and its server-side check:
 * <pre>
 * {"city": string|null, "state": string|null, "interests": [category], "dateFrom": string|null,
 *  "dateTo": string|null, "timeOfDay": string|null, "budgetMax": number|null, "notes": string|null}
 * </pre>
 * The check never trusts the model: a value of the wrong type becomes null, text is sanitized and capped. It does
 * not decide whether a value is valid (supported city, real date, ...); that is the caller's job.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExtractionResponseSchema {

    public static final String NAME = "event_filters";
    static final int SHORT_TEXT_MAX = 100;
    static final int NOTES_MAX = 500;
    static final int MAX_INTERESTS = 20;

    private static final List<String> FIELDS =
            List.of("city", "state", "interests", "dateFrom", "dateTo", "timeOfDay", "budgetMax", "notes");

    /**
     * {@code response_format} value for the chat completions request.
     *
     * @param categories the only values allowed in {@code interests}
     */
    public static Map<String, Object> responseFormat(List<String> categories) {
        Map<String, Object> nullableString = Map.of("type", List.of("string", "null"));
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("city", nullableString);
        properties.put("state", nullableString);
        properties.put("interests", Map.of("type", "array",
                "items", Map.of("type", "string", "enum", List.copyOf(categories))));
        properties.put("dateFrom", nullableString);
        properties.put("dateTo", nullableString);
        properties.put("timeOfDay", nullableString);
        properties.put("budgetMax", Map.of("type", List.of("number", "null")));
        properties.put("notes", nullableString);
        Map<String, Object> schema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", FIELDS,
                "properties", properties);
        return Map.of("type", "json_schema",
                "json_schema", Map.of("name", NAME, "strict", true, "schema", schema));
    }

    /**
     * The sanitized answer, or empty when {@code root} is not a JSON object.
     */
    public static Optional<AiExtraction> parse(JsonNode root) {
        if (root == null || !root.isObject()) {
            return Optional.empty();
        }
        return Optional.of(AiExtraction.builder()
                .city(text(root.get("city"), SHORT_TEXT_MAX))
                .state(text(root.get("state"), SHORT_TEXT_MAX))
                .interests(interests(root.get("interests")))
                .dateFrom(text(root.get("dateFrom"), SHORT_TEXT_MAX))
                .dateTo(text(root.get("dateTo"), SHORT_TEXT_MAX))
                .timeOfDay(text(root.get("timeOfDay"), SHORT_TEXT_MAX))
                .budgetMax(number(root.get("budgetMax")))
                .notes(text(root.get("notes"), NOTES_MAX))
                .build());
    }

    private static String text(JsonNode node, int maxLength) {
        if (node == null || !node.isString()) {
            return null;
        }
        String text = AiTextSanitizer.fromModel(node.stringValue(), maxLength);
        return text.isEmpty() ? null : text;
    }

    private static List<String> interests(JsonNode node) {
        List<String> interests = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return List.of();
        }
        for (int i = 0; i < node.size() && interests.size() < MAX_INTERESTS; i++) {
            String value = text(node.get(i), SHORT_TEXT_MAX);
            if (value != null) {
                interests.add(value);
            }
        }
        return List.copyOf(interests);
    }

    private static BigDecimal number(JsonNode node) {
        if (node == null || !node.isNumber()) {
            return null;
        }
        double value = node.numberValue().doubleValue();
        return Double.isFinite(value) ? node.decimalValue() : null;
    }
}
