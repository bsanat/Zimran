package kz.astyq.zimran.assistant.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A preference an assistant chat still needs ({@code pendingFields}), named like the chat's preference fields.
 */
@Getter
@RequiredArgsConstructor
public enum AssistantChatField {
    CITY("city"),
    INTERESTS("interests"),
    /** {@code startDate} and {@code endDate}. */
    DATES("dates");

    @JsonValue
    private final String id;
}
