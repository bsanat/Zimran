package kz.astyq.zimran.core.configuration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Google Gemini client settings. Calls go to Gemini's OpenAI-compatible chat completions endpoint, which keeps
 * strict JSON-schema structured output. {@code apiKey} (a free Google AI Studio key works) may be blank so the
 * app can boot without it; the planner then ranks with its rule-based fallback.
 * <p>
 * The frontend aborts {@code POST /api/plan} after 30 s and the backend must answer within 25 s, so the whole AI
 * call is capped by {@code totalTimeout} (at most 20 s), after which the fallback answers.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.gemini")
public class GeminiProperties {

    @NotBlank
    @URL
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai";

    private String apiKey;

    @NotBlank
    /** A free-tier model; any Gemini model id works. */
    private String model = "gemini-3.5-flash-lite";

    /**
     * {@code reasoning_effort} ({@code minimal}, {@code low}, {@code medium}, {@code high}); blank = not sent.
     */
    private String reasoningEffort = "low";

    @NotNull
    @DurationMin(millis = 1)
    private Duration connectTimeout = Duration.ofSeconds(3);

    @NotNull
    @DurationMin(millis = 1)
    private Duration readTimeout = Duration.ofSeconds(15);

    @NotNull
    @DurationMin(millis = 1)
    @DurationMax(seconds = 20)
    private Duration totalTimeout = Duration.ofSeconds(18);

    @Min(1)
    private int maxOutputTokens = 2000;

    /**
     * Gemini calls allowed per minute for the whole process; above it the planner uses the fallback. Kept low
     * for the free tier's per-minute quota.
     */
    @Min(1)
    private int maxRequestsPerMinute = 10;

    public boolean hasApiKey() {
        return StringUtils.hasText(apiKey);
    }

    @AssertTrue(message = "app.gemini.read-timeout must not exceed total-timeout")
    public boolean isReadTimeoutWithinTotal() {
        return readTimeout == null || totalTimeout == null || readTimeout.compareTo(totalTimeout) <= 0;
    }
}
