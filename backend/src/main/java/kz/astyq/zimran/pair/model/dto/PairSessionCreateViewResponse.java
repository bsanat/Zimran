package kz.astyq.zimran.pair.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.astyq.zimran.pair.model.enums.PairStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PairSessionCreateViewResponse {

    @Schema(example = "pair_Q2x9...")
    private String sessionId;

    @Schema(description = "Link to send to the other person; the invite token is in the fragment and works once")
    private String inviteUrl;

    @Schema(description = "The creator's secret; send it as X-Pair-Token. Shown only once")
    private String participantToken;

    @Schema(description = "When the invite expires")
    private Instant expiresAt;

    private PairStatus status;
}
