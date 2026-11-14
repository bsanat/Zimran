package kz.astyq.zimran.pair.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.astyq.zimran.pair.model.enums.PairRole;
import kz.astyq.zimran.pair.model.enums.PairStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * A session as one participant sees it.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PairSessionViewResponse {

    private String sessionId;
    private PairStatus status;
    @Schema(description = "Supported city id", example = "nyc")
    private String city;
    private String cityName;
    @Schema(description = "When the invite expires")
    private Instant expiresAt;
    private PairRole myRole;
    private PairParticipantViewResponse me;
    @Schema(description = "The other side's inputs without the free-text message; null until B joined")
    private PairParticipantViewResponse partner;
    private List<PairSuggestionViewResponse> suggestions;
    @Schema(description = "Set when MATCHED")
    private PairSharedPlanViewResponse sharedPlan;
    @Schema(description = "Set when NO_MATCH")
    private PairConflictViewResponse conflict;
}
