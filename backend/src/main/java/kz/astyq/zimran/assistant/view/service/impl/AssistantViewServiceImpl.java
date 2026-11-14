package kz.astyq.zimran.assistant.view.service.impl;

import kz.astyq.zimran.assistant.mapper.AssistantMapper;
import kz.astyq.zimran.assistant.model.domain.AssistantChatPreferences;
import kz.astyq.zimran.assistant.model.domain.AssistantFilters;
import kz.astyq.zimran.assistant.model.domain.AssistantRecommendation;
import kz.astyq.zimran.assistant.model.dto.AssistantChatPlanViewResponse;
import kz.astyq.zimran.assistant.model.dto.AssistantChatPreferencesRequest;
import kz.astyq.zimran.assistant.model.dto.AssistantChatRequest;
import kz.astyq.zimran.assistant.model.dto.AssistantChatViewResponse;
import kz.astyq.zimran.assistant.model.dto.AssistantParseRequest;
import kz.astyq.zimran.assistant.model.dto.AssistantParseViewResponse;
import kz.astyq.zimran.assistant.model.dto.AssistantRecommendRequest;
import kz.astyq.zimran.assistant.model.dto.AssistantRecommendViewResponse;
import kz.astyq.zimran.assistant.model.dto.RelaxationSuggestionViewResponse;
import kz.astyq.zimran.assistant.model.enums.AssistantChatField;
import kz.astyq.zimran.assistant.service.AssistantChatPreferenceService;
import kz.astyq.zimran.assistant.service.AssistantConversationService;
import kz.astyq.zimran.assistant.service.AssistantParseService;
import kz.astyq.zimran.assistant.service.AssistantRecommendationService;
import kz.astyq.zimran.assistant.service.AssistantRequestValidationService;
import kz.astyq.zimran.assistant.util.ClarificationQuestions;
import kz.astyq.zimran.assistant.util.MessageCode;
import kz.astyq.zimran.assistant.view.service.AssistantViewService;
import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.i18n.MessageService;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.model.enums.RateLimitAction;
import kz.astyq.zimran.core.service.RateLimitService;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    private final AssistantChatPreferenceService assistantChatPreferenceService;
    private final AssistantConversationService assistantConversationService;
    private final SupportedCityService supportedCityService;

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
        return assistantMapper.toViewResponse(recommendation, relaxations(recommendation));
    }

    @Override
    public AssistantChatViewResponse chat(AssistantChatRequest request, RequestScope scope) {
        rateLimitService.consume(RateLimitAction.AI_PARSE, scope);
        AssistantChatPreferences previous = assistantMapper.toDomain(request.getPreferences(),
                previousCity(request.getPreferences()));
        AssistantChatPreferences preferences = assistantChatPreferenceService.merge(request.getMessage(), previous);
        List<AssistantChatField> missing = assistantChatPreferenceService.missingFields(preferences);
        String conversationId = request.getConversationId();
        if (!missing.isEmpty()) {
            List<String> questions = ClarificationQuestions.chatMessageCode(missing)
                    .map(messageService::getMessage)
                    .map(List::of)
                    .orElse(List.of());
            return chatResponse(preferences, missing, questions, null, conversationId);
        }

        rateLimitService.consume(RateLimitAction.AI_RECOMMEND, scope);
        ValidatedPlanRequest validated = assistantRequestValidationService.validate(preferences, request.getMessage());
        boolean newConversation = conversationId == null;
        if (!newConversation) {
            // Counted before the AI call, so a turn that fails on the AI is still one of the chat's turns.
            assistantConversationService.continueConversation(conversationId, scope);
        }
        AssistantRecommendation recommendation = assistantRecommendationService.recommend(validated, scope,
                newConversation);
        // The quota was counted only if events were ranked; a chat without any match stays unopened.
        if (newConversation && !recommendation.getRecommendations().isEmpty()) {
            conversationId = assistantConversationService.start(scope);
        }
        AssistantChatPlanViewResponse result = assistantMapper.toChatPlanViewResponse(recommendation,
                relaxations(recommendation));
        return chatResponse(preferences, missing, List.of(), result, conversationId);
    }

    private SupportedCity previousCity(AssistantChatPreferencesRequest preferences) {
        if (preferences == null || !StringUtils.hasText(preferences.getCity())) {
            return null;
        }
        String city = preferences.getCity().strip();
        return supportedCityService.findById(city).or(() -> supportedCityService.findByName(city)).orElse(null);
    }

    private AssistantChatViewResponse chatResponse(AssistantChatPreferences preferences,
                                                   List<AssistantChatField> missing, List<String> questions,
                                                   AssistantChatPlanViewResponse result, String conversationId) {
        return AssistantChatViewResponse.builder()
                .engine(AssistantChatViewResponse.ENGINE)
                .preferences(assistantMapper.toViewResponse(preferences))
                .pendingFields(missing)
                .clarifications(questions)
                .result(result)
                .conversationId(conversationId)
                .build();
    }

    private List<RelaxationSuggestionViewResponse> relaxations(AssistantRecommendation recommendation) {
        return recommendation.getRelaxations().stream()
                .map(suggestion -> assistantMapper.toViewResponse(suggestion,
                        messageService.getMessage(MessageCode.relaxation(suggestion))))
                .toList();
    }
}
