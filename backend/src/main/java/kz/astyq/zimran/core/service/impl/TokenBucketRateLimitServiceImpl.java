package kz.astyq.zimran.core.service.impl;

import kz.astyq.zimran.core.configuration.RateLimitProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.model.enums.RateLimitAction;
import kz.astyq.zimran.core.service.RateLimitService;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.core.util.MessageCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token buckets keyed by action and caller. A bucket holds up to {@code capacity} permits and refills
 * {@code capacity} permits per {@code refill-period}, continuously. An action without configuration is not limited.
 * <p>
 * State is per process: with several instances each one limits on its own (swap for a Redis implementation then).
 */
@Service
@RequiredArgsConstructor
public class TokenBucketRateLimitServiceImpl implements RateLimitService {

    private final RateLimitProperties properties;
    private final Clock clock;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void consume(RateLimitAction action, RequestScope scope) {
        take(action, "scope:" + scope.getScopeId());
    }

    @Override
    public void consumeForClient(RateLimitAction action, String clientAddress) {
        take(action, "ip:" + clientAddress);
    }

    private void take(RateLimitAction action, String caller) {
        RateLimitProperties.Limit limit = properties.getActions().get(action);
        if (!properties.isEnabled() || limit == null) {
            return;
        }
        long now = clock.millis();
        evictIdle(now);
        double capacity = limit.getCapacity();
        double permitsPerMilli = capacity / limit.getRefillPeriod().toMillis();
        Bucket bucket = buckets.computeIfAbsent(action.getId() + "|" + caller, key -> new Bucket(capacity, now));
        long waitMillis = bucket.tryTake(now, capacity, permitsPerMilli);
        if (waitMillis > 0) {
            throw new ServiceValidationException(ErrorCode.RATE_LIMITED, MessageCode.RATE_LIMITED)
                    .withDetail("action", action.getId())
                    .withRetryAfter(Math.max(1, (waitMillis + 999) / 1000));
        }
    }

    /**
     * Keeps memory bounded: once too many callers are tracked, buckets idle long enough to be full again go.
     */
    private void evictIdle(long now) {
        if (buckets.size() <= properties.getMaxTrackedKeys()) {
            return;
        }
        long idleMillis = properties.getActions().values().stream()
                .mapToLong(limit -> limit.getRefillPeriod().toMillis())
                .max()
                .orElse(0);
        buckets.values().removeIf(bucket -> bucket.idleFor(now) >= idleMillis);
    }

    private static final class Bucket {

        private double permits;
        private long updatedAt;

        private Bucket(double permits, long updatedAt) {
            this.permits = permits;
            this.updatedAt = updatedAt;
        }

        /**
         * Refills, then takes one permit. Returns 0 on success, else the milliseconds until a permit is available.
         */
        private synchronized long tryTake(long now, double capacity, double permitsPerMilli) {
            long elapsed = Math.max(0, now - updatedAt);
            permits = Math.min(capacity, permits + elapsed * permitsPerMilli);
            updatedAt = Math.max(updatedAt, now);
            if (permits >= 1) {
                permits -= 1;
                return 0;
            }
            return Math.max(1, (long) Math.ceil((1 - permits) / permitsPerMilli));
        }

        private synchronized long idleFor(long now) {
            return now - updatedAt;
        }
    }
}
