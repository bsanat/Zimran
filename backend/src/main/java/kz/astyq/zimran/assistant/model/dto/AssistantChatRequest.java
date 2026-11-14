package kz.astyq.zimran.assistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class AssistantChatRequest {

    @Schema(description = "The guest's new chat message (untrusted text, read by the AI)",
            example = "Something with live music this Saturday evening", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 500)
    private String message;

    @Schema(description = "What the chat knew after the previous turn (its preferences); absent on the first turn")
    @Valid
    private AssistantChatPreferencesRequest preferences;

    @Schema(description = "The conversationId of an earlier result; absent until the chat has given one")
    @Size(max = 128)
    @Pattern(regexp = "^[A-Za-z0-9_-]{16,128}$")
    private String conversationId;
}
