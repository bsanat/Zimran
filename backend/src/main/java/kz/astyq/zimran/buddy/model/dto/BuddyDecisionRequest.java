package kz.astyq.zimran.buddy.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kz.astyq.zimran.buddy.model.enums.BuddyDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuddyDecisionRequest {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private BuddyDecision decision;
}
