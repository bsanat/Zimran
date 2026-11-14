package kz.astyq.zimran.ticketmaster.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class RequestThrottleTest {

    private final AtomicLong now = new AtomicLong();
    private final List<Long> sleeps = new ArrayList<>();
    private final RequestThrottle bucket = new RequestThrottle(5, Duration.ofSeconds(1), now::get, sleeps::add);

    @Test
    void burstOfFivePassesWithoutWaiting() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(bucket.tryAcquire()).isTrue();
        }
        assertThat(sleeps).isEmpty();
    }

    @Test
    void sixthPermitWaitsForRefill() throws Exception {
        for (int i = 0; i < 5; i++) {
            bucket.tryAcquire();
        }

        assertThat(bucket.tryAcquire()).isTrue();
        assertThat(sleeps).containsExactly(Duration.ofMillis(200).toNanos());
    }

    @Test
    void failsFastWhenWaitWouldExceedMaxWait() throws Exception {
        for (int i = 0; i < 10; i++) { // 5 immediate + 5 queued (200..1000 ms)
            assertThat(bucket.tryAcquire()).isTrue();
        }

        assertThat(bucket.tryAcquire()).isFalse(); // would wait 1200 ms
        assertThat(sleeps).hasSize(5).last().isEqualTo(Duration.ofSeconds(1).toNanos());
    }

    @Test
    void refillsOverTimeUpToCapacity() throws Exception {
        for (int i = 0; i < 5; i++) {
            bucket.tryAcquire();
        }
        now.addAndGet(Duration.ofSeconds(10).toNanos());

        for (int i = 0; i < 5; i++) {
            assertThat(bucket.tryAcquire()).isTrue();
        }
        assertThat(sleeps).isEmpty();
        assertThat(bucket.tryAcquire()).isTrue();
        assertThat(sleeps).hasSize(1);
    }
}
