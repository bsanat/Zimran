package kz.astyq.zimran.core.configuration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.planner")
public class PlannerProperties {

    /**
     * Max events sent to the AI for ranking.
     */
    @Positive
    private int maxCandidates = 30;

    /**
     * Max events returned to the client.
     */
    @Positive
    private int maxRecommendations = 5;

    /**
     * Max length of the requested date range, in days.
     */
    @Positive
    private int maxRangeDays = 7;

    /**
     * Whether events without a known price stay in the candidate set (shown as "price unknown").
     */
    private boolean includeUnknownPrice = true;

    @AssertTrue(message = "max-recommendations must not exceed max-candidates")
    public boolean isRecommendationsWithinCandidates() {
        return maxRecommendations <= maxCandidates;
    }
}
