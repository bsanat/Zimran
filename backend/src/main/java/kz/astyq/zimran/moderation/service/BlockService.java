package kz.astyq.zimran.moderation.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.moderation.model.entity.Block;
import kz.astyq.zimran.moderation.model.enums.ModerationTargetKind;

import java.util.Set;

/**
 * Blocks between scopes. A block works both ways: neither side sees the other's Buddy posts or can request.
 */
public interface BlockService {

    /**
     * Blocks the author of the {@code kind} item {@code targetId}. Idempotent (an existing block is returned).
     * Unknown item: {@code 404 NOT_FOUND}; own item: {@code 400 VALIDATION_ERROR} ({@code field = targetId}).
     * Publishes {@link kz.astyq.zimran.moderation.model.domain.ScopeBlockedEvent} for a new block.
     */
    Block block(RequestScope scope, ModerationTargetKind kind, String targetId);

    /**
     * Lifts the caller's block {@code scopeRef}; unknown or another scope's: {@code 404 NOT_FOUND}.
     */
    void unblock(RequestScope scope, String scopeRef);

    boolean isBlockedEitherWay(String firstScopeId, String secondScopeId);

    /**
     * Every scope {@code scopeId} blocked or was blocked by.
     */
    Set<String> blockedEitherWay(String scopeId);
}
