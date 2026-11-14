package kz.astyq.zimran.subscription.model.enums;

import lombok.Getter;

import java.util.Locale;

/**
 * Features with a usage quota ({@code app.subscription.quotas.<id>}).
 */
@Getter
public enum UsageFeature {
    /** An AI assistant session. */
    ASSISTANT,
    /** A Pair Planning session. */
    PAIR;

    /** Wire id, e.g. {@code assistant}; returned in {@code details.feature}. */
    private final String id = name().toLowerCase(Locale.ROOT);
}
