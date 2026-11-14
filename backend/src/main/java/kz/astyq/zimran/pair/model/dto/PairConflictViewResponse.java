package kz.astyq.zimran.pair.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.astyq.zimran.pair.model.enums.PairConflictType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Why there is no match ({@code NO_MATCH}) and which constraint to relax.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PairConflictViewResponse {

    @Schema(description = "DATES, BUDGET, INTERESTS or VOTES")
    private PairConflictType type;
    private String message;
    @Schema(description = "Constraint to relax: DATES, BUDGET or INTERESTS")
    private PairConflictType relax;
    private String relaxMessage;
}
