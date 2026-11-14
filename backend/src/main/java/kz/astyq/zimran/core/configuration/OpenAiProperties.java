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
 * OpenAI client settings. {@code apiKey} may be blank so the app can boot without it; the planner then ranks
 * with its rule-based fallback.
 * <p>
 * The frontend aborts {@code POST /api/plan} after 30 s and the backend must answer within 25 s, so the whole AI
 * call is capped by {@code totalTimeout} (at most 20 s), after which the fallback answers.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.openai")
public class OpenAiProperties {

    @NotBlank
    @URL
    private String baseUrl = "https://api.openai.com/v1";

    private String apiKey;

    @NotBlank
    private String model = "gpt-5-mini";

    /**
     * {@code reasoning_effort} sent to reasoning models ({@code minimal}, {@code low}, ...); blank = not sent.
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
     * OpenAI calls allowed per minute for the whole process; above it the planner uses the fallback.
     */
    @Min(1)
    private int maxRequestsPerMinute = 30;

    public boolean hasApiKey() {
        return StringUtils.hasText(apiKey);
    }

    @AssertTrue(message = "app.openai.read-timeout must not exceed total-timeout")
    public boolean isReadTimeoutWithinTotal() {
        return readTimeout == null || totalTimeout == null || readTimeout.compareTo(totalTimeout) <= 0;
    }
}
