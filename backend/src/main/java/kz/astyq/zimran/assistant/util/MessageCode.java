package kz.astyq.zimran.assistant.util;

import kz.astyq.zimran.assistant.model.enums.RelaxationSuggestion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * AI assistant message keys in {@code messages*.properties}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageCode {

    public static final String CLARIFY_CITY_AND_DATES = "assistant_clarify_city_and_dates";
    public static final String CLARIFY_CITY = "assistant_clarify_city";
    public static final String CLARIFY_DATES = "assistant_clarify_dates";
    public static final String CLARIFY_INTERESTS = "assistant_clarify_interests";

    private static final String RELAXATION_PREFIX = "assistant_relax_";

    /**
     * Key of a relaxation suggestion's text, e.g. {@code assistant_relax_increase_budget}.
     */
    public static String relaxation(RelaxationSuggestion suggestion) {
        return RELAXATION_PREFIX + suggestion.getId();
    }
}
