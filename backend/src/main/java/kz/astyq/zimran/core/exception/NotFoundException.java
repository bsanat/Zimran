package kz.astyq.zimran.core.exception;

import kz.astyq.zimran.core.util.ErrorCode;

import java.util.Map;

public class NotFoundException extends ApiException {

    public NotFoundException() {
        this(ErrorCode.NOT_FOUND.getDefaultMessageKey(), Map.of());
    }

    public NotFoundException(String messageKey, Object... args) {
        this(messageKey, Map.of(), args);
    }

    public NotFoundException(String messageKey, Map<String, Object> details, Object... args) {
        super(ErrorCode.NOT_FOUND, messageKey, details, args);
    }
}
