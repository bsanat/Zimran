package kz.astyq.zimran.core.exception;

import kz.astyq.zimran.core.util.ErrorCode;

import java.util.Map;

/**
 * The AI provider failed, timed out or returned an answer that did not pass validation.
 */
public class AiUnavailableException extends ApiException {

    public AiUnavailableException() {
        this((Throwable) null);
    }

    public AiUnavailableException(Throwable cause) {
        this(ErrorCode.AI_UNAVAILABLE.getDefaultMessageKey(), Map.of(), cause);
    }

    public AiUnavailableException(String messageKey, Map<String, Object> details, Throwable cause,
                                  Object... args) {
        super(ErrorCode.AI_UNAVAILABLE, messageKey, details, cause, args);
    }
}
