package kz.astyq.zimran.assistant.util;

import kz.astyq.zimran.assistant.model.enums.AssistantChatField;
import kz.astyq.zimran.assistant.model.enums.AssistantField;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Picks the follow-up question for filters that could not be read. The question is always a fixed message key,
 * never model text.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClarificationQuestions {

    /**
     * The same question for an assistant chat's missing fields.
     */
    public static Optional<String> chatMessageCode(Collection<AssistantChatField> missingFields) {
        Set<AssistantField> fields = EnumSet.noneOf(AssistantField.class);
        missingFields.forEach(field -> fields.add(switch (field) {
            case CITY -> AssistantField.CITY;
            case INTERESTS -> AssistantField.INTERESTS;
            case DATES -> AssistantField.DATE_FROM;
        }));
        return messageCode(fields);
    }

    /**
     * City and dates first (both needed for any search), then interests; empty when nothing required is missing.
     */
    public static Optional<String> messageCode(Collection<AssistantField> missingFields) {
        boolean city = missingFields.contains(AssistantField.CITY);
        boolean dates = missingFields.contains(AssistantField.DATE_FROM) || missingFields.contains(AssistantField.DATE_TO);
        if (city && dates) {
            return Optional.of(MessageCode.CLARIFY_CITY_AND_DATES);
        }
        if (city) {
            return Optional.of(MessageCode.CLARIFY_CITY);
        }
        if (dates) {
            return Optional.of(MessageCode.CLARIFY_DATES);
        }
        if (missingFields.contains(AssistantField.INTERESTS)) {
            return Optional.of(MessageCode.CLARIFY_INTERESTS);
        }
        return Optional.empty();
    }
}
