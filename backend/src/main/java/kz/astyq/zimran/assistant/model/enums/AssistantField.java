package kz.astyq.zimran.assistant.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A filter of the AI assistant. Over the wire (in {@code missingFields}) it is the request property name.
 */
@Getter
@RequiredArgsConstructor
public enum AssistantField {
    CITY("city"),
    INTERESTS("interests"),
    DATE_FROM("dateFrom"),
    DATE_TO("dateTo"),
    TIME_OF_DAY("timeOfDay"),
    BUDGET_MAX("budgetMax");

    @JsonValue
    private final String id;
}
