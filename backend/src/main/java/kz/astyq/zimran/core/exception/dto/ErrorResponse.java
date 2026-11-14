package kz.astyq.zimran.core.exception.dto;

/**
 * Error envelope: {@code {"error":{"code":"...","message":"...","details":{...},"requestId":"..."}}}.
 */
public record ErrorResponse(ErrorBody error) {
}
