package kz.astyq.zimran.demo.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Demo message keys in {@code messages*.properties}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageCode {

    public static final String DEMO_DISABLED = "demo_disabled";
    public static final String DEMO_PREMIUM_REQUIRED = "demo_premium_required";
    /** Default title of a demo event. Argument {0}: the category label. */
    public static final String DEMO_EVENT_TITLE = "demo_event_title";
    public static final String DEMO_START_IN_PAST = "demo_start_in_past";
}
