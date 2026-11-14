package kz.astyq.zimran.assistant.view.service;

import kz.astyq.zimran.assistant.model.dto.AssistantParseRequest;
import kz.astyq.zimran.assistant.model.dto.AssistantParseViewResponse;
import kz.astyq.zimran.assistant.model.dto.AssistantRecommendRequest;
import kz.astyq.zimran.assistant.model.dto.AssistantRecommendViewResponse;
import kz.astyq.zimran.core.model.domain.RequestScope;

/**
 * The AI event assistant: free text to filters, filters to AI-ranked events.
 */
public interface AssistantViewService {

    /**
     * Rate limited per scope, or per client address without one ({@code 429 RATE_LIMITED}). Reads and validates
     * the filters; a clarification question (i18n text) when city, dates or interests are missing. AI unavailable:
     * {@code 503 AI_UNAVAILABLE}.
     *
     * @param scope         the caller's scope, or null for a caller without a guest session
     * @param clientAddress the caller's address, for rate limiting without a scope
     */
    AssistantParseViewResponse parse(AssistantParseRequest request, RequestScope scope, String clientAddress);

    /**
     * Rate limited per scope; validates the filters ({@code 400 VALIDATION_ERROR} with {@code details.field}),
     * then recommends within the scope's {@code assistant} quota. Relaxation texts follow the request language.
     */
    AssistantRecommendViewResponse recommend(AssistantRecommendRequest request, RequestScope scope);
}
