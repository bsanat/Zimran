package kz.astyq.zimran.subscription.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

/**
 * Quota windows are aligned to the epoch so every use in a window maps to the same {@code window_start}
 * (which is what makes the atomic upsert possible). A {@code null} window is a single window forever.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class QuotaWindows {

    public static Instant start(Instant now, Duration window) {
        if (window == null) {
            return Instant.EPOCH;
        }
        long size = window.toMillis();
        return Instant.ofEpochMilli(Math.floorDiv(now.toEpochMilli(), size) * size);
    }

    /**
     * When the window containing {@code now} ends, or {@code null} for a window that never ends.
     */
    public static Instant end(Instant now, Duration window) {
        return window == null ? null : start(now, window).plus(window);
    }
}
