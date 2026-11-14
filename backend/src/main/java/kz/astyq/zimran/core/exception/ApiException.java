package kz.astyq.zimran.core.exception;

import kz.astyq.zimran.core.util.ErrorCode;
import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Base class for all errors that map to a client-facing {@link ErrorCode}.
 * <p>
 * {@code messageKey} is an i18n key resolved by {@code MessageService} with {@code args};
 * {@code details} is returned to the client as-is, so never put secrets or internal data in it.
 */
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final String messageKey;
    private final Map<String, Object> details;
    private final Object[] args;

    public ApiException(ErrorCode code, String messageKey, Map<String, Object> details, Object... args) {
        this(code, messageKey, details, null, args);
    }

    protected ApiException(ErrorCode code, String messageKey, Map<String, Object> details,
                           Throwable cause, Object[] args) {
        super(messageKey, cause);
        this.code = Objects.requireNonNull(code, "code");
        this.messageKey = messageKey != null ? messageKey : code.getDefaultMessageKey();
        this.details = details == null || details.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(details));
        this.args = args != null ? args.clone() : new Object[0];
    }

    public Object[] getArgs() {
        return args.clone();
    }
}
