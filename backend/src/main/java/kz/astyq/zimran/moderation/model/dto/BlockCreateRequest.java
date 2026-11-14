package kz.astyq.zimran.moderation.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.astyq.zimran.moderation.model.enums.ModerationTargetKind;
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
public class BlockCreateRequest {

    @Schema(description = "POST blocks the post's owner, REQUEST the request's sender",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private ModerationTargetKind targetType;

    @Schema(description = "Public id of the Buddy post or request", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String targetId;
}
