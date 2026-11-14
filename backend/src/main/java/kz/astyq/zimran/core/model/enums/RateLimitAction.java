package kz.astyq.zimran.core.model.enums;

import lombok.Getter;

import java.util.Locale;

/**
 * Actions with their own rate limit ({@code app.rate-limit.actions.<id>}).
 */
@Getter
public enum RateLimitAction {
    GUEST_SESSION,
    AI_PARSE,
    AI_RECOMMEND,
    PAIR_CREATE,
    BUDDY_POST,
    BUDDY_REQUEST,
    ALERTS_CHECK;

    /** Wire id, e.g. {@code ai_parse}; returned in {@code details.action}. */
    private final String id = name().toLowerCase(Locale.ROOT);
}
