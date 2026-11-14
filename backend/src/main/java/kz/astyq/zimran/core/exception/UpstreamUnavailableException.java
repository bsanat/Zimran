package kz.astyq.zimran.core.exception;

import kz.astyq.zimran.core.util.ErrorCode;

import java.util.Map;

/**
 * An external data provider (e.g. Ticketmaster) failed, timed out or returned garbage.
 */
public class UpstreamUnavailableException extends ApiException {

    public UpstreamUnavailableException() {
        this((Throwable) null);
    }

    public UpstreamUnavailableException(Throwable cause) {
        this(ErrorCode.UPSTREAM_UNAVAILABLE.getDefaultMessageKey(), Map.of(), cause);
    }

    public UpstreamUnavailableException(String messageKey, Map<String, Object> details, Throwable cause,
                                        Object... args) {
        super(ErrorCode.UPSTREAM_UNAVAILABLE, messageKey, details, cause, args);
    }
}
