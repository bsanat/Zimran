package kz.astyq.zimran.core.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * OpenAI client settings. {@code apiKey} may be blank so the app can boot without it.
 */
@Validated
@ConfigurationProperties(prefix = "app.openai")
public record OpenAiProperties(
        @NotBlank @URL @DefaultValue("https://api.openai.com/v1") String baseUrl,
        String apiKey,
        @NotBlank String model,
        @NotNull @DurationMin(millis = 1) @DefaultValue("3s") Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("30s") Duration readTimeout,
        @Min(1) @DefaultValue("2000") int maxOutputTokens
) {

    public boolean hasApiKey() {
        return StringUtils.hasText(apiKey);
    }

    @Override
    public String toString() {
        return "OpenAiProperties[baseUrl=" + baseUrl + ", apiKey=" + (hasApiKey() ? "****" : "<blank>")
                + ", model=" + model + ", connectTimeout=" + connectTimeout + ", readTimeout=" + readTimeout
                + ", maxOutputTokens=" + maxOutputTokens + "]";
    }
}
