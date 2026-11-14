package kz.astyq.zimran.jsonsource.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * JSON event source message keys in {@code messages*.properties}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageCode {

    public static final String EVENTS_SOURCE_UNAVAILABLE = "events_source_unavailable";
    public static final String EVENTS_SOURCE_NOT_CONFIGURED = "events_source_not_configured";
}
