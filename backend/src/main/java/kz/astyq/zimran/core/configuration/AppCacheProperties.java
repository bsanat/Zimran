package kz.astyq.zimran.core.configuration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * In-memory cache settings ({@code app.cache}). See {@link CacheConfig} for the caches themselves.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.cache")
public class AppCacheProperties {

    /**
     * How long one Ticketmaster search result is served from {@code eventSearch}.
     */
    @NotNull
    @DurationMin(seconds = 1)
    private Duration searchTtl = Duration.ofMinutes(10);

    @Positive
    private long searchMaxSize = 500;

    /**
     * How long a search result stays in {@code eventSearchStale}, used only when Ticketmaster is down.
     */
    @NotNull
    @DurationMin(seconds = 1)
    private Duration staleTtl = Duration.ofHours(6);

    @Positive
    private long staleMaxSize = 500;

    @NotNull
    @DurationMin(seconds = 1)
    private Duration byIdTtl = Duration.ofMinutes(30);

    @Positive
    private long byIdMaxSize = 5000;
}
