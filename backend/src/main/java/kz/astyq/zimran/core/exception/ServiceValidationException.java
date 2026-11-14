package kz.astyq.zimran.core.exception;

import kz.astyq.zimran.core.util.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The single application exception. {@code errorCode} decides the HTTP status and the client-facing
 * code; {@code messageCode} is a {@code messages*.properties} key resolved with {@code arguments}.
 * <p>
 * {@code details} is returned to the client as-is, so never put secrets or internal data in it.
 * {@code retryAfterSeconds}, when set, is sent as the {@code Retry-After} header.
 */
@Getter
public class ServiceValidationException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String messageCode;
    private final Object[] arguments;
    private final Map<String, Object> details = new LinkedHashMap<>();
    private Long retryAfterSeconds;

    public ServiceValidationException(ErrorCode errorCode, String messageCode, Object... arguments) {
        super(messageCode);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
        this.messageCode = Objects.requireNonNull(messageCode, "messageCode");
        this.arguments = arguments != null ? arguments.clone() : new Object[0];
    }

    public ServiceValidationException(ErrorCode errorCode, String messageCode) {
        this(errorCode, messageCode, new Object[0]);
    }

    public HttpStatus getStatus() {
        return errorCode.getStatus();
    }

    public Object[] getArguments() {
        return arguments.clone();
    }

    public Map<String, Object> getDetails() {
        return Collections.unmodifiableMap(details);
    }

    public ServiceValidationException withDetail(String key, Object value) {
        details.put(Objects.requireNonNull(key, "key"), value);
        return this;
    }

    public ServiceValidationException withRetryAfter(long seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("retryAfterSeconds must be >= 0");
        }
        this.retryAfterSeconds = seconds;
        return this;
    }

    /**
     * Attaches a cause for server-side logs only; it is never returned to the client.
     */
    public ServiceValidationException withCause(Throwable cause) {
        initCause(cause);
        return this;
    }
}
