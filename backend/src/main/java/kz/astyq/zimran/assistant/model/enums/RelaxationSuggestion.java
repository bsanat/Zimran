package kz.astyq.zimran.assistant.model.enums;

import lombok.Getter;

import java.util.Locale;

/**
 * A way to loosen the filters when nothing matched. Over the wire: its lowercase {@link #getId() id}
 * ({@code increase_budget}); the text is the message {@code assistant_relax_<id>}.
 */
@Getter
public enum RelaxationSuggestion {
    INCREASE_BUDGET,
    WIDEN_DATES,
    MORE_INTERESTS;

    private final String id = name().toLowerCase(Locale.ROOT);
}
