package kz.astyq.zimran.assistant.view.service.impl;

import kz.astyq.zimran.assistant.mapper.AssistantMapper;
import kz.astyq.zimran.assistant.model.domain.AssistantFilters;
import kz.astyq.zimran.assistant.model.domain.AssistantRecommendation;
import kz.astyq.zimran.assistant.model.dto.AssistantParseRequest;
import kz.astyq.zimran.assistant.model.dto.AssistantParseViewResponse;
import kz.astyq.zimran.assistant.model.dto.AssistantRecommendRequest;
import kz.astyq.zimran.assistant.model.dto.AssistantRecommendViewResponse;
import kz.astyq.zimran.assistant.model.dto.RelaxationSuggestionViewResponse;
import kz.astyq.zimran.assistant.service.AssistantParseService;
import kz.astyq.zimran.assistant.service.AssistantRecommendationService;
import kz.astyq.zimran.assistant.service.AssistantRequestValidationService;
import kz.astyq.zimran.assistant.util.ClarificationQuestions;
import kz.astyq.zimran.assistant.util.MessageCode;
import kz.astyq.zimran.assistant.view.service.AssistantViewService;
import kz.astyq.zimran.core.i18n.MessageService;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.model.enums.RateLimitAction;
import kz.astyq.zimran.core.service.RateLimitService;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssistantViewServiceImpl implements AssistantViewService {

    private final RateLimitService rateLimitService;
    private final AssistantParseService assistantParseService;
    private final AssistantRequestValidationService assistantRequestValidationService;
    private final AssistantRecommendationService assistantRecommendationService;
    private final AssistantMapper assistantMapper;
    private final MessageService messageService;

    @Override
    public AssistantParseViewResponse parse(AssistantParseRequest request, RequestScope scope, String clientAddress) {
        if (scope != null) {
            rateLimitService.consume(RateLimitAction.AI_PARSE, scope);
        } else {
            rateLimitService.consumeForClient(RateLimitAction.AI_PARSE, clientAddress);
        }
        AssistantFilters filters = assistantParseService.parse(request.getMessage(), request.getCity());
        String question = ClarificationQuestions.messageCode(filters.getMissingFields())
                .map(messageService::getMessage)
                .orElse(null);
        return assistantMapper.toViewResponse(filters, question);
    }

    @Override
    public AssistantRecommendViewResponse recommend(AssistantRecommendRequest request, RequestScope scope) {
        rateLimitService.consume(RateLimitAction.AI_RECOMMEND, scope);
        ValidatedPlanRequest validated = assistantRequestValidationService.validate(request);
        AssistantRecommendation recommendation = assistantRecommendationService.recommend(validated, scope);
        List<RelaxationSuggestionViewResponse> relaxations = recommendation.getRelaxations().stream()
                .map(suggestion -> assistantMapper.toViewResponse(suggestion,
                        messageService.getMessage(MessageCode.relaxation(suggestion))))
                .toList();
        return assistantMapper.toViewResponse(recommendation, relaxations);
    }
}
