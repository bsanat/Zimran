package kz.astyq.zimran.core.web;

import java.io.IOException;

/**
 * Thrown while reading a request body that exceeds {@code app.web.max-request-body-bytes}; the exception handler
 * turns it into {@code 413 PAYLOAD_TOO_LARGE}.
 */
public class RequestBodyTooLargeException extends IOException {

    public RequestBodyTooLargeException(long limit) {
        super("Request body exceeds " + limit + " bytes");
    }
}
