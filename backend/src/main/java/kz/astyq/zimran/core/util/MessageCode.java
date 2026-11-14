package kz.astyq.zimran.core.util;

/**
 * Keys of the shared error messages in {@code messages*.properties}, resolved by {@code MessageService}.
 */
public final class MessageCode {

    public static final String VALIDATION_ERROR = "validation_error";
    public static final String FIELD_INVALID = "field_invalid";
    public static final String MALFORMED_REQUEST = "malformed_request";
    public static final String NOT_FOUND = "not_found";
    /** Argument {0}: the HTTP method. */
    public static final String METHOD_NOT_ALLOWED = "method_not_allowed";
    public static final String INTERNAL_ERROR = "internal_error";

    private MessageCode() {
    }
}
