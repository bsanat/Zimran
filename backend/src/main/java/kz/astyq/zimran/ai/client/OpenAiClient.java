package kz.astyq.zimran.ai.client;

import kz.astyq.zimran.ai.model.dto.ChatCompletionRequest;

/**
 * OpenAI chat completions.
 */
public interface OpenAiClient {

    /**
     * Content of the first choice. No API key, HTTP or network error, timeout, refusal, truncated or empty
     * answer: {@code ServiceValidationException(AI_UNAVAILABLE)}. The API key is never logged.
     */
    String complete(ChatCompletionRequest request);
}
