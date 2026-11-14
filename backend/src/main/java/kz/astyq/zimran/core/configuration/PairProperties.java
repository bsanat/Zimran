package kz.astyq.zimran.core.configuration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Pair Planning ({@code app.pair}).
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.pair")
public class PairProperties {

    /**
     * How long the invite stays open; after it an unjoined session is EXPIRED.
     */
    @NotNull
    @DurationMin(minutes = 1)
    private Duration inviteTtl = Duration.ofHours(48);

    /**
     * Suggestions computed when B joins: at most {@code maxSuggestions}; fewer than {@code minSuggestions} is
     * still a result when that is all that fits both sides.
     */
    @Min(1)
    @Max(20)
    private int minSuggestions = 3;

    @Min(1)
    @Max(20)
    private int maxSuggestions = 5;

    @AssertTrue(message = "app.pair.min-suggestions must not exceed app.pair.max-suggestions")
    public boolean isSuggestionRangeValid() {
        return minSuggestions <= maxSuggestions;
    }
}
