package kz.astyq.zimran.subscription.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Uses left in the current window of the caller's tier (the free quota, or the Premium fair-use limit).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionLimitsViewResponse {

    @Schema(example = "1")
    private Integer assistantSessionsLeft;

    @Schema(example = "1")
    private Integer pairSessionsLeft;
}
