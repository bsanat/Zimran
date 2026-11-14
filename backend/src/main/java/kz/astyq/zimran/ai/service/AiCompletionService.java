package kz.astyq.zimran.ai.service;

import kz.astyq.zimran.ai.model.dto.ChatMessage;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * One structured-output call to the model, shared by every AI feature.
 */
public interface AiCompletionService {

    /**
     * Sends {@code messages} with the strict {@code responseFormat} and returns the answer parsed as JSON (its
     * shape is not checked yet). No API key, process usage cap reached ({@link AiUsageGuard}), client error,
     * {@code app.gemini.total-timeout} exceeded or a non-JSON answer:
     * {@code ServiceValidationException(AI_UNAVAILABLE)}.
     */
    JsonNode complete(List<ChatMessage> messages, Map<String, Object> responseFormat);
}
