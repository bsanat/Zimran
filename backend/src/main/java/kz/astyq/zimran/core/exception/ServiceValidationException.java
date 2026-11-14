package kz.astyq.zimran.core.exception;

import kz.astyq.zimran.core.util.ErrorCode;

import java.util.Map;

/**
 * Legacy validation exception kept for source compatibility. Prefer {@link ValidationException}
 * (or another {@link ApiException} subclass) in new code.
 */
public class ServiceValidationException extends ValidationException {

    public ServiceValidationException(ErrorCode errorCode, String message, Object... arguments) {
        super(errorCode, message, Map.of(), arguments);
    }

    public ServiceValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message, Map.of());
    }

    public ErrorCode getErrorCode() {
        return getCode();
    }

    public Object[] getArguments() {
        return getArgs();
    }
}
