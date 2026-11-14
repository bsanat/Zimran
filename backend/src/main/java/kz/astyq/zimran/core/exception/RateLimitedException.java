package kz.astyq.zimran.core.exception;

import kz.astyq.zimran.core.util.ErrorCode;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Too many requests. {@code retryAfterSeconds} is returned in the {@code Retry-After} header
 * and in {@code details.retryAfterSeconds}.
 */
@Getter
public class RateLimitedException extends ApiException {

    private final long retryAfterSeconds;

    public RateLimitedException(long retryAfterSeconds) {
        this(retryAfterSeconds, ErrorCode.RATE_LIMITED.getDefaultMessageKey(), Map.of());
    }

    public RateLimitedException(long retryAfterSeconds, String messageKey, Map<String, Object> details,
                                Object... args) {
        super(ErrorCode.RATE_LIMITED, messageKey, withRetryAfter(details, retryAfterSeconds), args);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    private static Map<String, Object> withRetryAfter(Map<String, Object> details, long retryAfterSeconds) {
        if (retryAfterSeconds < 0) {
            throw new IllegalArgumentException("retryAfterSeconds must be >= 0");
        }
        Map<String, Object> merged = new LinkedHashMap<>(details != null ? details : Map.of());
        merged.put("retryAfterSeconds", retryAfterSeconds);
        return merged;
    }
}
