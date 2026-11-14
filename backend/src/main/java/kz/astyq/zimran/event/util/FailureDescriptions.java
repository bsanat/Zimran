package kz.astyq.zimran.event.util;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Log-safe description of a failure: the error code or the exception type, never its message (messages of HTTP
 * client errors can contain request URLs with API keys).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FailureDescriptions {

    public static String describe(Throwable failure) {
        if (failure instanceof ServiceValidationException sve) {
            return sve.getErrorCode().name();
        }
        return failure.getClass().getSimpleName();
    }
}
