package kz.astyq.zimran.core.web;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitInterceptorTest {

    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");

    @Test
    @DisplayName("over the per-minute limit: RATE_LIMITED with Retry-After; other clients are not affected")
    void limit() {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(2, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(call(interceptor, "1.1.1.1")).isTrue();
        assertThat(call(interceptor, "1.1.1.1")).isTrue();
        assertThatThrownBy(() -> call(interceptor, "1.1.1.1"))
                .isInstanceOfSatisfying(ServiceValidationException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RATE_LIMITED);
                    assertThat(e.getRetryAfterSeconds()).isEqualTo(60);
                });
        assertThat(call(interceptor, "2.2.2.2")).isTrue();
    }

    @Test
    @DisplayName("a new minute starts a new window")
    void newWindow() {
        Instant[] now = {NOW};
        Clock clock = new Clock() {
            @Override
            public java.time.ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now[0];
            }
        };
        RateLimitInterceptor interceptor = new RateLimitInterceptor(1, clock);

        assertThat(call(interceptor, "1.1.1.1")).isTrue();
        now[0] = NOW.plus(Duration.ofSeconds(59));
        assertThatThrownBy(() -> call(interceptor, "1.1.1.1")).isInstanceOf(ServiceValidationException.class);
        now[0] = NOW.plus(Duration.ofSeconds(60));
        assertThat(call(interceptor, "1.1.1.1")).isTrue();
    }

    private static boolean call(RateLimitInterceptor interceptor, String address) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/plan");
        request.setRemoteAddr(address);
        return interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
    }
}
