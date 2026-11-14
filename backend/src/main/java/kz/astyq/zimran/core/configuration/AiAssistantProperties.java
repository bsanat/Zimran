package kz.astyq.zimran.core.configuration;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * AI assistant settings ({@code app.ai}); the model connection itself is {@code app.openai}.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.ai")
public class AiAssistantProperties {

    /**
     * Max events sent to the AI for one assistant recommendation (also capped by
     * {@code app.planner.max-candidates}).
     */
    @Positive
    private int maxCandidates = 20;
}
