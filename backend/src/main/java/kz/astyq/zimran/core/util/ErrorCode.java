package kz.astyq.zimran.core.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Stable, client-facing error codes, each with the HTTP status it is returned with.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    GUEST_TOKEN_INVALID(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    PREMIUM_REQUIRED(HttpStatus.FORBIDDEN),
    FREE_LIMIT_REACHED(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    CONFLICT(HttpStatus.CONFLICT),
    PAIR_ALREADY_JOINED(HttpStatus.CONFLICT),
    PAIR_INVITE_EXPIRED(HttpStatus.GONE),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    UPSTREAM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    /**
     * Best-effort mapping of an arbitrary HTTP status (e.g. from a framework exception) to a code.
     */
    public static ErrorCode fromStatus(HttpStatusCode status) {
        return switch (status.value()) {
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 409 -> CONFLICT;
            case 413 -> PAYLOAD_TOO_LARGE;
            case 429 -> RATE_LIMITED;
            case 503 -> UPSTREAM_UNAVAILABLE;
            default -> status.is4xxClientError() ? VALIDATION_ERROR : INTERNAL_ERROR;
        };
    }
}
