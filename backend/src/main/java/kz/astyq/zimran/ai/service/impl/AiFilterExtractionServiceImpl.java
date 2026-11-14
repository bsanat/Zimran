package kz.astyq.zimran.ai.service.impl;

import kz.astyq.zimran.ai.model.domain.AiExtraction;
import kz.astyq.zimran.ai.model.domain.AiExtractionRequest;
import kz.astyq.zimran.ai.service.AiCompletionService;
import kz.astyq.zimran.ai.service.AiFilterExtractionService;
import kz.astyq.zimran.ai.util.ExtractionPromptBuilder;
import kz.astyq.zimran.ai.util.ExtractionResponseSchema;
import kz.astyq.zimran.ai.util.MessageCode;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiFilterExtractionServiceImpl implements AiFilterExtractionService {

    private final AiCompletionService aiCompletionService;

    @Override
    public AiExtraction extract(AiExtractionRequest request) {
        Objects.requireNonNull(request, "request");
        return ExtractionResponseSchema.parse(aiCompletionService.complete(
                        ExtractionPromptBuilder.messages(request),
                        ExtractionResponseSchema.responseFormat(request.getCategories())))
                .orElseThrow(() -> {
                    log.warn("AI filter extraction answer does not match the schema");
                    return new ServiceValidationException(ErrorCode.AI_UNAVAILABLE, MessageCode.AI_UNAVAILABLE);
                });
    }
}
