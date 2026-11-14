package kz.astyq.zimran.alert.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Alert message keys in {@code messages*.properties}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageCode {

    /** Argument {0}: the channel id. */
    public static final String CHANNEL_NOT_AVAILABLE = "alert_channel_not_available";
}
