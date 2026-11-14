package kz.astyq.zimran.ai.service.impl;

import kz.astyq.zimran.ai.model.domain.AiRanking;
import kz.astyq.zimran.ai.model.domain.AiRankingRequest;
import kz.astyq.zimran.ai.service.AiCompletionService;
import kz.astyq.zimran.ai.service.AiRankingService;
import kz.astyq.zimran.ai.util.RankingPromptBuilder;
import kz.astyq.zimran.ai.util.RankingResponseSchema;
import kz.astyq.zimran.core.configuration.OpenAiProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRankingServiceImpl implements AiRankingService {

    private final AiCompletionService aiCompletionService;
    private final OpenAiProperties properties;
    private final JsonMapper jsonMapper;

    @Override
    public Optional<AiRanking> rank(AiRankingRequest request) {
        if (request == null || request.getCandidates().isEmpty() || !properties.hasApiKey()) {
            return Optional.empty();
        }
        JsonNode answer;
        try {
            answer = aiCompletionService.complete(RankingPromptBuilder.messages(request, jsonMapper),
                    RankingResponseSchema.responseFormat(request.isWithSuggestions()));
        } catch (ServiceValidationException e) {
            // Already logged by the completion service; the caller falls back.
            return Optional.empty();
        }
        Optional<AiRanking> ranking = RankingResponseSchema.parse(answer);
        if (ranking.isEmpty()) {
            log.warn("AI ranking answer does not match the schema");
        }
        return ranking;
    }
}
