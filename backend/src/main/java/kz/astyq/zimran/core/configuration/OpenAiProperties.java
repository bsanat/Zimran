package kz.astyq.zimran.core.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * OpenAI client settings. {@code apiKey} may be blank so the app can boot without it.
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

    @NotNull
    @DurationMin(millis = 1)
    private Duration connectTimeout = Duration.ofSeconds(3);

    @NotNull
    @DurationMin(millis = 1)
    private Duration readTimeout = Duration.ofSeconds(30);

    @Min(1)
    private int maxOutputTokens = 2000;

    public boolean hasApiKey() {
        return StringUtils.hasText(apiKey);
    }
}
