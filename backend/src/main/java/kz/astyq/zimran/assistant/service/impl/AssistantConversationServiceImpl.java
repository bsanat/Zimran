package kz.astyq.zimran.assistant.service.impl;

import kz.astyq.zimran.assistant.model.entity.AssistantConversation;
import kz.astyq.zimran.assistant.repository.AssistantConversationRepository;
import kz.astyq.zimran.assistant.service.AssistantConversationService;
import kz.astyq.zimran.assistant.util.MessageCode;
import kz.astyq.zimran.core.configuration.AiAssistantProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.util.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssistantConversationServiceImpl implements AssistantConversationService {

    private final AssistantConversationRepository repository;
    private final AiAssistantProperties properties;
    private final Clock clock;

    @Override
    @Transactional
    public String start(RequestScope scope) {
        Instant now = clock.instant();
        AssistantConversation conversation = repository.save(AssistantConversation.builder()
                .publicId(UUID.randomUUID().toString())
                .scopeId(scope.getScopeId())
                .turns(1)
                .expiresAt(now.plus(properties.getConversationTtl()))
                .build());
        return conversation.getPublicId();
    }

    @Override
    @Transactional
    public void continueConversation(String publicId, RequestScope scope) {
        // Another scope's conversation looks the same as an unknown one.
        AssistantConversation conversation = repository.findByPublicId(publicId)
                .filter(found -> found.getScopeId().equals(scope.getScopeId()))
                .orElseThrow(() -> new ServiceValidationException(ErrorCode.ASSISTANT_CONVERSATION_NOT_FOUND,
                        MessageCode.CONVERSATION_NOT_FOUND));
        Instant now = clock.instant();
        if (!conversation.getExpiresAt().isAfter(now)) {
            throw new ServiceValidationException(ErrorCode.ASSISTANT_CONVERSATION_EXPIRED,
                    MessageCode.CONVERSATION_EXPIRED);
        }
        int maxTurns = properties.getMaxConversationTurns();
        if (repository.incrementTurns(conversation.getId(), maxTurns, now) == 0) {
            throw new ServiceValidationException(ErrorCode.ASSISTANT_TURN_LIMIT, MessageCode.TURN_LIMIT, maxTurns);
        }
    }
}
