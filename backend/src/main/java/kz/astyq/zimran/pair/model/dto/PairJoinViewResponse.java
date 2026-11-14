package kz.astyq.zimran.pair.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.astyq.zimran.pair.model.enums.PairStatus;
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
public class PairJoinViewResponse {

    private String sessionId;

    @Schema(description = "The invitee's secret; send it as X-Pair-Token. Shown only once")
    private String participantToken;

    @Schema(description = "READY with suggestions, or NO_MATCH when no event fits both sides")
    private PairStatus status;
}
