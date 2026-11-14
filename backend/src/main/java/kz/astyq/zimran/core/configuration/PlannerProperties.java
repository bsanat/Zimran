package kz.astyq.zimran.core.configuration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * @param maxCandidates      max events sent to the AI for ranking
 * @param maxRecommendations max events returned to the client
 * @param maxRangeDays       max length of the requested date range, in days
 */
@Validated
@ConfigurationProperties(prefix = "app.planner")
public record PlannerProperties(
        @Positive @DefaultValue("30") int maxCandidates,
        @Positive @DefaultValue("5") int maxRecommendations,
        @Positive @DefaultValue("7") int maxRangeDays
) {

    @AssertTrue(message = "max-recommendations must not exceed max-candidates")
    public boolean isRecommendationsWithinCandidates() {
        return maxRecommendations <= maxCandidates;
    }
}
