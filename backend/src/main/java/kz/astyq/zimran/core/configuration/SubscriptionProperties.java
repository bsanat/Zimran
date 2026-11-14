package kz.astyq.zimran.core.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import kz.astyq.zimran.subscription.model.enums.UsageFeature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * The Premium plan and the usage quotas per tier ({@code app.subscription}). The quota numbers are product
 * hypotheses: tune them here, not in code.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.subscription")
public class SubscriptionProperties {

    @Valid
    @NotNull
    private Plan plan = new Plan();

    private Map<UsageFeature, @Valid @NotNull Quota> quotas = new EnumMap<>(UsageFeature.class);

    @AssertTrue(message = "app.subscription.quotas must configure every usage feature")
    public boolean isEveryFeatureConfigured() {
        return quotas.keySet().containsAll(EnumSet.allOf(UsageFeature.class));
    }

    public Quota quotaFor(UsageFeature feature) {
        return quotas.get(feature);
    }

    @Getter
    @Setter
    public static class Plan {

        @Min(0)
        private int priceCents = 999;

        @NotBlank
        @Pattern(regexp = "[A-Z]{3}")
        private String currency = "USD";

        @NotBlank
        private String interval = "month";

        /**
         * Feature ids; each label is the message {@code subscription_feature_<id>}.
         */
        private List<@NotBlank String> features = new ArrayList<>();
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Quota {

        @Valid
        @NotNull
        private Tier free;

        /**
         * Fair-use limit for Premium.
         */
        @Valid
        @NotNull
        private Tier premium;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tier {

        /**
         * Uses allowed per window; 0 disables the feature for the tier.
         */
        @Min(0)
        private int limit;

        /**
         * Window length; windows are aligned to the epoch. {@code null} = one window forever (a one-time trial).
         */
        @DurationMin(minutes = 1)
        private Duration window;
    }
}
