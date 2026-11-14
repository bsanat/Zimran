package kz.astyq.zimran.buddy.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class BuddyPostCreateRequest {

    @Schema(description = "Public event id", example = "json:ny-prospect-park-chess", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 300)
    private String eventId;

    @Schema(description = "Shown to others; no contact details", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(min = 2, max = 50)
    private String displayName;

    @Schema(description = "A short introduction; no contact details", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 300)
    private String shortBio;

    @Schema(description = "Conversation language", example = "English")
    @Size(max = 32)
    private String language;

    @Schema(description = "What would make it a good plan; no contact details")
    @Size(max = 300)
    private String preferences;

    @Schema(description = "Preferred company age range, both 18 or older")
    @Min(18)
    @Max(120)
    private Integer ageMin;

    @Min(18)
    @Max(120)
    private Integer ageMax;

    @Schema(description = "Must be true: the guest confirms being 18 or older", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean ageConfirmed;
}
