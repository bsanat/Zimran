package kz.astyq.zimran.event.model.domain;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.util.MessageCode;
import lombok.NonNull;
import lombok.Value;

/**
 * Identity of an event: the source plus the source's own id. Public form: {@code <source>:<sourceEventId>},
 * e.g. {@code json:evt-123}.
 */
@Value
public class EventId {

    private static final char SEPARATOR = ':';

    @NonNull
    EventSource source;

    @NonNull
    String sourceId;

    /**
     * Parses the public form. Unknown source, missing separator or blank source id:
     * {@code ServiceValidationException(NOT_FOUND, EVENT_NOT_FOUND)}.
     */
    public static EventId parse(String value) {
        int separator = value != null ? value.indexOf(SEPARATOR) : -1;
        if (separator <= 0 || separator == value.length() - 1) {
            throw notFound();
        }
        String sourceId = value.substring(separator + 1);
        if (sourceId.isBlank()) {
            throw notFound();
        }
        return EventSource.fromCode(value.substring(0, separator))
                .map(source -> new EventId(source, sourceId))
                .orElseThrow(EventId::notFound);
    }

    /**
     * The public form, the inverse of {@link #parse}.
     */
    public String asString() {
        return source.getCode() + SEPARATOR + sourceId;
    }

    private static ServiceValidationException notFound() {
        return new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.EVENT_NOT_FOUND);
    }
}
