package kz.astyq.zimran.moderation.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.astyq.zimran.moderation.model.enums.ReportReason;
import kz.astyq.zimran.moderation.model.enums.ReportTargetType;
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
public class ReportCreateRequest {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private ReportTargetType targetType;

    @Schema(description = "Public id of the Buddy post or request (for SCOPE: one written by the reported person)",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String targetId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private ReportReason reason;

    @Size(max = 500)
    private String comment;
}
