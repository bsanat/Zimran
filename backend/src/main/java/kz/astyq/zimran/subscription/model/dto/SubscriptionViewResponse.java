package kz.astyq.zimran.subscription.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.astyq.zimran.subscription.model.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionViewResponse {

    private SubscriptionStatus status;

    @Schema(description = "true for DEMO_PREMIUM and PREMIUM_ACTIVE")
    private Boolean isPremium;

    @Schema(description = "true when Premium comes from the demo, without payment")
    private Boolean demoOnly;

    @Schema(description = "End of the paid period; null when not applicable", nullable = true)
    private Instant periodEnd;

    private SubscriptionPlanViewResponse plan;

    private SubscriptionLimitsViewResponse limits;
}
