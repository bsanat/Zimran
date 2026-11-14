package kz.astyq.zimran.ai.service.impl;

import kz.astyq.zimran.ai.service.AiUsageGuard;
import kz.astyq.zimran.core.configuration.GeminiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;

/**
 * Fixed one-minute window.
 */
@Service
@RequiredArgsConstructor
public class AiUsageGuardImpl implements AiUsageGuard {

    static final long WINDOW_MILLIS = 60_000;

    private final GeminiProperties properties;
    private final Clock clock;

    private long windowStart = Long.MIN_VALUE;
    private int used;

    @Override
    public synchronized boolean tryAcquire() {
        long now = clock.millis();
        if (windowStart == Long.MIN_VALUE || now - windowStart >= WINDOW_MILLIS) {
            windowStart = now;
            used = 0;
        }
        if (used >= properties.getMaxRequestsPerMinute()) {
            return false;
        }
        used++;
        return true;
    }
}
