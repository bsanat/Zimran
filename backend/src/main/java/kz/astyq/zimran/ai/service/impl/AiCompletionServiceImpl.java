package kz.astyq.zimran.ai.service.impl;

import kz.astyq.zimran.ai.client.OpenAiClient;
import kz.astyq.zimran.ai.model.dto.ChatCompletionRequest;
import kz.astyq.zimran.ai.model.dto.ChatMessage;
import kz.astyq.zimran.ai.service.AiCompletionService;
import kz.astyq.zimran.ai.service.AiUsageGuard;
import kz.astyq.zimran.ai.util.MessageCode;
import kz.astyq.zimran.core.configuration.OpenAiProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs the OpenAI call on a virtual thread so the whole call (connect, upload, model time, download) is bounded
 * by {@code app.openai.total-timeout}; the per-read timeout alone would not bound a slow answer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCompletionServiceImpl implements AiCompletionService {

    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final OpenAiClient openAiClient;
    private final AiUsageGuard aiUsageGuard;
    private final OpenAiProperties properties;
    private final JsonMapper jsonMapper;

    @Override
    public JsonNode complete(List<ChatMessage> messages, Map<String, Object> responseFormat) {
        if (!properties.hasApiKey()) {
            throw unavailable(null);
        }
        if (!aiUsageGuard.tryAcquire()) {
            log.info("AI usage cap reached");
            throw unavailable(null);
        }
        ChatCompletionRequest completion = ChatCompletionRequest.builder()
                .model(properties.getModel())
                .messages(messages)
                .responseFormat(responseFormat)
                .maxCompletionTokens(properties.getMaxOutputTokens())
                .reasoningEffort(StringUtils.hasText(properties.getReasoningEffort()) ? properties.getReasoningEffort() : null)
                .build();

        Future<String> call = CompletableFuture.supplyAsync(() -> openAiClient.complete(completion), EXECUTOR);
        String content;
        try {
            content = call.get(properties.getTotalTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            call.cancel(true);
            log.warn("AI call timed out after {}", properties.getTotalTimeout());
            throw unavailable(e);
        } catch (InterruptedException e) {
            call.cancel(true);
            Thread.currentThread().interrupt();
            throw unavailable(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.warn("AI call failed: {}", cause.toString(), cause.getCause());
            throw unavailable(cause);
        }
        try {
            return jsonMapper.readTree(content);
        } catch (JacksonException e) {
            log.warn("AI answer is not JSON");
            throw unavailable(null);
        }
    }

    private static ServiceValidationException unavailable(Throwable cause) {
        ServiceValidationException exception = new ServiceValidationException(ErrorCode.AI_UNAVAILABLE,
                MessageCode.AI_UNAVAILABLE);
        return cause != null ? exception.withCause(cause) : exception;
    }
}
