package kz.astyq.zimran.core.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import kz.astyq.zimran.core.model.enums.RateLimitAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/**
 * Per-action rate limits ({@code app.rate-limit}). An action without an entry is not limited.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    /**
     * Above this many tracked (action, caller) pairs, idle ones are dropped.
     */
    @Positive
    private int maxTrackedKeys = 50_000;

    private Map<RateLimitAction, @Valid @NotNull Limit> actions = new EnumMap<>(RateLimitAction.class);

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Limit {

        /**
         * Burst size: permits available to an idle caller.
         */
        @Positive
        private int capacity;

        /**
         * Time to refill {@code capacity} permits (refill is continuous).
         */
        @NotNull
        @DurationMin(millis = 1)
        private Duration refillPeriod;
    }
}
