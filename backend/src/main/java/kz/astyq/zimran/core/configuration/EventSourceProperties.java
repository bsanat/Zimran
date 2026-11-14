package kz.astyq.zimran.core.configuration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import kz.astyq.zimran.event.model.enums.Interest;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The JSON event feed ({@code app.events-source}). {@code location} may be blank so the app boots without it;
 * the catalog then answers {@code 503 UPSTREAM_UNAVAILABLE}.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.events-source")
public class EventSourceProperties {

    /**
     * File path or {@code http(s)://} URL of the JSON document (env {@code EVENTS_JSON_SOURCE}).
     */
    private String location;

    /**
     * How long a loaded document is served before the source is checked again. A file is re-read only when its
     * size or modification time changed; a URL is fetched again.
     */
    @NotNull
    @DurationMin(seconds = 1)
    private Duration refreshInterval = Duration.ofMinutes(1);

    @NotNull
    @DurationMin(millis = 1)
    private Duration connectTimeout = Duration.ofSeconds(3);

    @NotNull
    @DurationMin(millis = 1)
    private Duration readTimeout = Duration.ofSeconds(10);

    /**
     * Largest accepted document, in bytes.
     */
    @Positive
    private long maxBytes = 50L * 1024 * 1024;

    /**
     * Source category names (compared ignoring case) per interest. A source category equal to an interest id
     * ({@code music}) needs no entry. Events whose categories map to no interest are skipped.
     */
    private Map<Interest, List<String>> categoryMapping = new LinkedHashMap<>();

    public boolean isConfigured() {
        return StringUtils.hasText(location);
    }

    public boolean isUrl() {
        String value = location != null ? location.strip().toLowerCase(Locale.ROOT) : "";
        return value.startsWith("http://") || value.startsWith("https://");
    }

    public List<String> categoriesFor(Interest interest) {
        List<String> names = categoryMapping.get(interest);
        return names != null ? names : new ArrayList<>();
    }
}
