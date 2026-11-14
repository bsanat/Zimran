package kz.astyq.zimran.core.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Event catalog ingestion ({@code app.ingestion}).
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.ingestion")
public class IngestionProperties {

    /** Whether the scheduled job runs. The live fetch of an empty city works either way. */
    private boolean enabled = true;

    /** Spring cron (6 fields) of the job that ingests every supported city. */
    @NotBlank
    private String cron = "0 */15 * * * *";

    /**
     * Least time between two live fetches of one city whose catalog is empty, so a city the provider has no
     * events for is not fetched on every request.
     */
    @NotNull
    @DurationMin(seconds = 1)
    private Duration liveFetchCooldown = Duration.ofMinutes(1);
}
