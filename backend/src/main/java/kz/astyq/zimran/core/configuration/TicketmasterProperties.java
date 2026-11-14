package kz.astyq.zimran.core.configuration;

import jakarta.validation.constraints.Max;
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
 * Ticketmaster Discovery API client settings. {@code apiKey} may be blank so the app can boot without it.
 */
@Validated
@ConfigurationProperties(prefix = "app.ticketmaster")
public record TicketmasterProperties(
        @NotBlank @URL @DefaultValue("https://app.ticketmaster.com/discovery/v2") String baseUrl,
        String apiKey,
        @NotNull @DurationMin(millis = 1) @DefaultValue("3s") Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("10s") Duration readTimeout,
        @Min(1) @Max(200) @DefaultValue("100") int pageSize
) {

    public boolean hasApiKey() {
        return StringUtils.hasText(apiKey);
    }

    @Override
    public String toString() {
        return "TicketmasterProperties[baseUrl=" + baseUrl + ", apiKey=" + (hasApiKey() ? "****" : "<blank>")
                + ", connectTimeout=" + connectTimeout + ", readTimeout=" + readTimeout
                + ", pageSize=" + pageSize + "]";
    }
}
