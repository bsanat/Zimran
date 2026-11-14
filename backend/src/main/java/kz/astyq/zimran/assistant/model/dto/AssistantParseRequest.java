package kz.astyq.zimran.assistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body of {@code POST /api/ai/parse}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantParseRequest {

    @Schema(description = "What the guest wants, in their own words",
            example = "I have $60 this Saturday evening in NYC. I like jazz and cozy places.",
            requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 500)
    @NotBlank
    @Size(max = 500)
    private String message;

    @Schema(description = "City the guest is looking at (id or name); used when the message names no city",
            example = "nyc", maxLength = 100)
    @Size(max = 100)
    private String city;
}
