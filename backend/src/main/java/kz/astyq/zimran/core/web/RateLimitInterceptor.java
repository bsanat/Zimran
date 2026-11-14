package kz.astyq.zimran.core.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.core.util.MessageCode;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed one-minute window per client (remote address). Over the limit:
 * {@code ServiceValidationException(RATE_LIMITED)} with {@code Retry-After} set to the rest of the window.
 * <p>
 * State is per process. Behind a proxy every client shares the proxy's address, so keep the limit generous.
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    static final long WINDOW_MILLIS = 60_000;
    static final int MAX_TRACKED_CLIENTS = 10_000;

    private final int requestsPerMinute;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitInterceptor(int requestsPerMinute, Clock clock) {
        this.requestsPerMinute = requestsPerMinute;
        this.clock = clock;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        long now = clock.millis();
        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.values().removeIf(window -> now - window.start >= WINDOW_MILLIS);
        }
        String client = String.valueOf(request.getRemoteAddr());
        Window window = windows.compute(client, (key, current) ->
                current == null || now - current.start >= WINDOW_MILLIS ? new Window(now, 1) : current.next());
        if (window.count > requestsPerMinute) {
            long retryAfter = Math.max(1, (window.start + WINDOW_MILLIS - now + 999) / 1000);
            throw new ServiceValidationException(ErrorCode.RATE_LIMITED, MessageCode.RATE_LIMITED)
                    .withRetryAfter(retryAfter);
        }
        return true;
    }

    private static final class Window {
        private final long start;
        private final int count;

        private Window(long start, int count) {
            this.start = start;
            this.count = count;
        }

        private Window next() {
            return new Window(start, count + 1);
        }
    }
}
