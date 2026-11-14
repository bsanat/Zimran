package kz.astyq.zimran.core.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.model.enums.RateLimitAction;

/**
 * Per-action rate limits. Each call takes one permit; over the limit it throws
 * {@code ServiceValidationException(RATE_LIMITED)} with {@code Retry-After} and {@code details.action}.
 * Implementations may keep state in memory or in a shared store (Redis) without changing callers.
 */
public interface RateLimitService {

    /**
     * Limits the action per caller scope.
     */
    void consume(RateLimitAction action, RequestScope scope);

    /**
     * Limits the action per client address, for calls made before the caller has a scope.
     */
    void consumeForClient(RateLimitAction action, String clientAddress);
}
