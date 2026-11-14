package kz.astyq.zimran.pair.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body of {@code POST /api/pair/join}: the invite token from the link and the invitee's (side B) wishes.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PairJoinCreateRequest {

    @Schema(description = "Token from the invite link", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 128)
    private String inviteToken;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull
    private PairPreferencesRequest participantB;
}
