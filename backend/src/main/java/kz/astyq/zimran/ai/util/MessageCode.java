package kz.astyq.zimran.ai.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * AI message keys in {@code messages*.properties}. The planner falls back when AI fails; the AI assistant
 * answers {@code 503 AI_UNAVAILABLE} with this message.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageCode {

    public static final String AI_UNAVAILABLE = "ai_unavailable";
}
