package kz.astyq.zimran.ai.service.impl;

import kz.astyq.zimran.ai.model.domain.AiPairReason;
import kz.astyq.zimran.ai.model.domain.AiPairReasonRequest;
import kz.astyq.zimran.ai.service.AiCompletionService;
import kz.astyq.zimran.ai.service.AiPairReasonService;
import kz.astyq.zimran.ai.util.PairReasonPromptBuilder;
import kz.astyq.zimran.ai.util.PairReasonResponseSchema;
import kz.astyq.zimran.core.configuration.OpenAiProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPairReasonServiceImpl implements AiPairReasonService {

    private final AiCompletionService aiCompletionService;
    private final OpenAiProperties properties;
    private final JsonMapper jsonMapper;

    @Override
    public Optional<List<AiPairReason>> explain(AiPairReasonRequest request) {
        if (request == null || request.getEvents().isEmpty() || !properties.hasApiKey()) {
            return Optional.empty();
        }
        JsonNode answer;
        try {
            answer = aiCompletionService.complete(PairReasonPromptBuilder.messages(request, jsonMapper),
                    PairReasonResponseSchema.responseFormat());
        } catch (ServiceValidationException e) {
            // Already logged by the completion service; the caller falls back to template reasons.
            return Optional.empty();
        }
        Optional<List<AiPairReason>> reasons = PairReasonResponseSchema.parse(answer);
        if (reasons.isEmpty()) {
            log.warn("AI pair reason answer does not match the schema");
        }
        return reasons;
    }
}
