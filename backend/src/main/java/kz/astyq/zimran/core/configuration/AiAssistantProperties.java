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
 * AI assistant settings ({@code app.ai}); the model connection itself is {@code app.gemini}.
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

    /**
     * How long an assistant chat ({@code POST /api/ai/chat}) stays open after its first result.
     */
    @NotNull
    @DurationMin(minutes = 1)
    private Duration conversationTtl = Duration.ofHours(24);

    /**
     * Results one assistant chat may give; the quota is counted once per chat.
     */
    @Positive
    private int maxConversationTurns = 30;
}
