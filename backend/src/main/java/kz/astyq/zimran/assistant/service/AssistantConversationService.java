package kz.astyq.zimran.assistant.service;

import kz.astyq.zimran.core.model.domain.RequestScope;

/**
 * Assistant chats of a scope. The assistant quota is counted when a chat starts; later results in it are turns.
 */
public interface AssistantConversationService {

    /**
     * Opens a new conversation for the scope with its first turn counted, open for {@code app.ai.conversation-ttl}.
     *
     * @return the public id the client sends back
     */
    String start(RequestScope scope);

    /**
     * Counts one more turn of the scope's conversation {@code publicId}. Unknown or another scope's:
     * {@code 404 ASSISTANT_CONVERSATION_NOT_FOUND}; expired: {@code 410 ASSISTANT_CONVERSATION_EXPIRED}; at
     * {@code app.ai.max-conversation-turns}: {@code 429 ASSISTANT_TURN_LIMIT}.
     */
    void continueConversation(String publicId, RequestScope scope);
}
