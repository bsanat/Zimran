package kz.astyq.zimran.pair.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.astyq.zimran.event.model.dto.EventViewResponse;
import kz.astyq.zimran.pair.model.enums.PairVoteType;
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
public class PairSuggestionViewResponse {

    @Schema(description = "1 = best")
    private int rank;
    @Schema(description = "Catalog event; every field comes from the event source")
    private EventViewResponse event;
    @Schema(description = "Why it suits side A")
    private String reasonA;
    @Schema(description = "Why it suits side B")
    private String reasonB;
    @Schema(description = "Null when the event matches an interest both sides share")
    private PairCompromiseViewResponse compromise;
    private PairVoteType myVote;
    @Schema(description = "The other side's vote; null until the caller voted on this event")
    private PairVoteType partnerVote;
}
