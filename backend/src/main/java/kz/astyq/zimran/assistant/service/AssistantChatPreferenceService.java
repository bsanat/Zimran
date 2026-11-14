package kz.astyq.zimran.assistant.service;

import kz.astyq.zimran.assistant.model.domain.AssistantChatPreferences;
import kz.astyq.zimran.assistant.model.enums.AssistantChatField;

import java.util.List;

/**
 * One chat turn's understanding: the AI reads the new message, the server validates it and merges it over what
 * the chat already knew.
 */
public interface AssistantChatPreferenceService {

    /**
     * The AI (Gemini) extracts filters from {@code message} only; every value is validated like
     * {@code POST /api/ai/parse}. A value the message states replaces the previous one; anything it does not
     * mention is kept. The previous city is the reference city for "today" and is kept unless the message names
     * another one (an unsupported city named in the message clears it). AI unavailable:
     * {@code 503 AI_UNAVAILABLE}.
     */
    AssistantChatPreferences merge(String message, AssistantChatPreferences previous);

    /**
     * The fields still needed before events can be recommended: city, interests and dates.
     */
    List<AssistantChatField> missingFields(AssistantChatPreferences preferences);
}
