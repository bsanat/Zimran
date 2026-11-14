package kz.astyq.zimran.pair.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.astyq.zimran.pair.model.enums.PairVoteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body of {@code POST /api/pair/{sessionId}/votes}. A new vote on the same event replaces the previous one.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PairVoteCreateRequest {

    @Schema(description = "Id of one of the session's suggested events", example = "json:evt-123",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 300)
    private String eventId;

    @Schema(description = "LIKE, MAYBE or PASS", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private PairVoteType vote;
}
