package kz.astyq.zimran.ai.service.impl;

import kz.astyq.zimran.ai.client.OpenAiClient;
import kz.astyq.zimran.ai.model.domain.AiRanking;
import kz.astyq.zimran.ai.model.domain.AiRankingRequest;
import kz.astyq.zimran.ai.model.dto.ChatCompletionRequest;
import kz.astyq.zimran.ai.service.AiRankingService;
import kz.astyq.zimran.ai.service.AiUsageGuard;
import kz.astyq.zimran.ai.util.RankingPromptBuilder;
import kz.astyq.zimran.ai.util.RankingResponseSchema;
import kz.astyq.zimran.core.configuration.OpenAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
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
public class AiRankingServiceImpl implements AiRankingService {

    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final OpenAiClient openAiClient;
    private final AiUsageGuard aiUsageGuard;
    private final OpenAiProperties properties;
    private final JsonMapper jsonMapper;

    @Override
    public Optional<AiRanking> rank(AiRankingRequest request) {
        if (request == null || request.getCandidates().isEmpty() || !properties.hasApiKey()) {
            return Optional.empty();
        }
        if (!aiUsageGuard.tryAcquire()) {
            log.info("AI usage cap reached; using the rule-based ranking");
            return Optional.empty();
        }
        ChatCompletionRequest completion = ChatCompletionRequest.builder()
                .model(properties.getModel())
                .messages(RankingPromptBuilder.messages(request, jsonMapper))
                .responseFormat(RankingResponseSchema.responseFormat())
                .maxCompletionTokens(properties.getMaxOutputTokens())
                .reasoningEffort(StringUtils.hasText(properties.getReasoningEffort()) ? properties.getReasoningEffort() : null)
                .build();

        Future<String> call = CompletableFuture.supplyAsync(() -> openAiClient.complete(completion), EXECUTOR);
        String content;
        try {
            content = call.get(properties.getTotalTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            call.cancel(true);
            log.warn("AI ranking timed out after {}", properties.getTotalTimeout());
            return Optional.empty();
        } catch (InterruptedException e) {
            call.cancel(true);
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (ExecutionException e) {
            log.warn("AI ranking failed: {}", e.getCause() != null ? e.getCause().toString() : e.toString(),
                    e.getCause() != null ? e.getCause().getCause() : null);
            return Optional.empty();
        }
        try {
            Optional<AiRanking> ranking = RankingResponseSchema.parse(jsonMapper.readTree(content));
            if (ranking.isEmpty()) {
                log.warn("AI ranking answer does not match the schema");
            }
            return ranking;
        } catch (JacksonException e) {
            log.warn("AI ranking answer is not JSON");
            return Optional.empty();
        }
    }
}
