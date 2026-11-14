package kz.astyq.zimran.ai.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * AI message keys in {@code messages*.properties}. AI errors never reach the client (the planner falls back),
 * the key only names the failure in logs.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageCode {

    public static final String AI_UNAVAILABLE = "ai_unavailable";
}
