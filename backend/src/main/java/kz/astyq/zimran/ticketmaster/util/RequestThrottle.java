package kz.astyq.zimran.ticketmaster.util;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Minimal thread-safe token bucket ({@code permitsPerSecond} refill rate, burst of the same size),
 * implemented as a GCRA with integer nanosecond math. A caller that finds the bucket empty reserves
 * the next permit and sleeps until it is due, unless that wait would exceed {@code maxWait}: then
 * nothing is reserved and {@link #tryAcquire()} returns {@code false} so the caller can fail fast.
 */
public class RequestThrottle {

    @FunctionalInterface
    public interface Sleeper {
        void sleepNanos(long nanos) throws InterruptedException;
    }

    private final long intervalNanos;
    private final long burstToleranceNanos;
    private final long maxWaitNanos;
    private final LongSupplier nanoClock;
    private final Sleeper sleeper;

    /** Theoretical arrival time of the next permit if the bucket were drained at exactly the rate. */
    private long nextPermitAt;

    public RequestThrottle(int permitsPerSecond, Duration maxWait) {
        this(permitsPerSecond, maxWait, System::nanoTime, TimeUnit.NANOSECONDS::sleep);
    }

    public RequestThrottle(int permitsPerSecond, Duration maxWait, LongSupplier nanoClock, Sleeper sleeper) {
        if (permitsPerSecond <= 0) {
            throw new IllegalArgumentException("permitsPerSecond must be > 0");
        }
        this.intervalNanos = TimeUnit.SECONDS.toNanos(1) / permitsPerSecond;
        this.burstToleranceNanos = intervalNanos * (permitsPerSecond - 1);
        this.maxWaitNanos = maxWait.toNanos();
        this.nanoClock = nanoClock;
        this.sleeper = sleeper;
        this.nextPermitAt = nanoClock.getAsLong();
    }

    /**
     * Max time {@link #tryAcquire()} waits for a permit.
     */
    public Duration getMaxWait() {
        return Duration.ofNanos(maxWaitNanos);
    }

    /**
     * Takes one permit, waiting up to {@code maxWait} for it.
     *
     * @return {@code false} if no permit can be obtained within {@code maxWait}
     * @throws InterruptedException if interrupted while waiting (the reserved permit is not returned)
     */
    public boolean tryAcquire() throws InterruptedException {
        long waitNanos;
        synchronized (this) {
            long now = nanoClock.getAsLong();
            long tat = Math.max(nextPermitAt, now);
            waitNanos = Math.max(0, tat - now - burstToleranceNanos);
            if (waitNanos > maxWaitNanos) {
                return false;
            }
            nextPermitAt = tat + intervalNanos;
        }
        if (waitNanos > 0) {
            sleeper.sleepNanos(waitNanos);
        }
        return true;
    }
}
