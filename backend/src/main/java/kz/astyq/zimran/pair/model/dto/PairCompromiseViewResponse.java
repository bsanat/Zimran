package kz.astyq.zimran.pair.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.astyq.zimran.pair.model.enums.PairRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The event matches only one side's interests.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PairCompromiseViewResponse {

    @Schema(description = "Side whose interest the event serves")
    private PairRole favors;
    private String message;
}
