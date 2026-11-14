package kz.astyq.zimran.core.exception;

import kz.astyq.zimran.core.util.ErrorCode;

import java.util.Map;

public class ValidationException extends ApiException {

    public ValidationException(String messageKey, Object... args) {
        this(messageKey, Map.of(), args);
    }

    public ValidationException(String messageKey, Map<String, Object> details, Object... args) {
        this(ErrorCode.VALIDATION_ERROR, messageKey, details, args);
    }

    /**
     * Allows legacy subclasses ({@link ServiceValidationException}) to carry an arbitrary code.
     */
    protected ValidationException(ErrorCode code, String messageKey, Map<String, Object> details, Object... args) {
        super(code, messageKey, details, args);
    }
}
