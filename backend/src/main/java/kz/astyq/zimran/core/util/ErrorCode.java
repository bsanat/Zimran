package kz.astyq.zimran.core.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Stable, client-facing error codes. Each code carries the HTTP status it is returned with
 * and the i18n key of its default message.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "error.validation"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "error.unauthorized"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "error.forbidden"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "error.not-found"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "error.rate-limited"),
    UPSTREAM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "error.upstream-unavailable"),
    AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "error.ai-unavailable"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal");

    private final HttpStatus status;
    private final String defaultMessageKey;

    /**
     * Best-effort mapping of an arbitrary HTTP status (e.g. from a framework exception) to a code.
     */
    public static ErrorCode fromStatus(HttpStatusCode status) {
        return switch (status.value()) {
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 429 -> RATE_LIMITED;
            case 503 -> UPSTREAM_UNAVAILABLE;
            default -> status.is4xxClientError() ? VALIDATION_ERROR : INTERNAL_ERROR;
        };
    }
}
