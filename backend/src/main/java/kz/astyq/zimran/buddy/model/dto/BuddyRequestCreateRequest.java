package kz.astyq.zimran.buddy.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class BuddyRequestCreateRequest {

    @Schema(description = "Shown to the post owner; no contact details", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(min = 2, max = 50)
    private String displayName;

    @Schema(description = "Hello to the post owner; no contact details", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 300)
    private String message;

    @Schema(description = "Must be true: the guest confirms being 18 or older", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean ageConfirmed;
}
