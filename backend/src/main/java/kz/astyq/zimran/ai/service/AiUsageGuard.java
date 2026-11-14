package kz.astyq.zimran.ai.service;

/**
 * Caps OpenAI calls per minute for the whole process ({@code app.openai.max-requests-per-minute}) so a burst of
 * plans cannot run up cost; calls over the cap use the rule-based ranking instead.
 */
public interface AiUsageGuard {

    /**
     * Takes one call from the current minute's budget; false when it is used up.
     */
    boolean tryAcquire();
}
