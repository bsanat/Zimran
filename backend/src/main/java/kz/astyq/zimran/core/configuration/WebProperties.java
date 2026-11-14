package kz.astyq.zimran.core.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * HTTP guard rails ({@code app.web}): request body size and per-client rate limits.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.web")
public class WebProperties {

    /**
     * Largest accepted request body, in bytes; bigger bodies get {@code 413 PAYLOAD_TOO_LARGE}.
     */
    @Positive
    private long maxRequestBodyBytes = 10_240;

    @Valid
    @NotNull
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class RateLimit {

        private boolean enabled = true;

        /**
         * Requests one client (remote address) may send per minute to {@link #paths}; more get {@code 429}.
         */
        @Positive
        private int requestsPerMinute = 30;

        /**
         * Spring MVC path patterns the limit applies to.
         */
        @NotNull
        private List<String> paths = new ArrayList<>(List.of("/api/plan"));
    }
}
