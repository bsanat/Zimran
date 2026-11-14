package kz.astyq.zimran.core.configuration;

import jakarta.validation.constraints.Max;
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
 * Ticketmaster Discovery API client settings. {@code apiKey} may be blank so the app can boot without it.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.ticketmaster")
public class TicketmasterProperties {

    @NotBlank
    @URL
    private String baseUrl = "https://app.ticketmaster.com/discovery/v2";

    private String apiKey;

    @NotNull
    @DurationMin(millis = 1)
    private Duration connectTimeout = Duration.ofSeconds(3);

    @NotNull
    @DurationMin(millis = 1)
    private Duration readTimeout = Duration.ofSeconds(10);

    @Min(1)
    @Max(200)
    private int pageSize = 100;

    public boolean hasApiKey() {
        return StringUtils.hasText(apiKey);
    }
}
