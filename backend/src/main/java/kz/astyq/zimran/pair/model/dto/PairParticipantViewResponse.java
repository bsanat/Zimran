package kz.astyq.zimran.pair.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.astyq.zimran.pair.model.enums.PairRole;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * One side's inputs. {@code message} is only filled for the caller's own side.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PairParticipantViewResponse {

    private PairRole role;
    @Schema(description = "Interest ids")
    private List<String> interests;
    private BigDecimal budgetMax;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private TimePreference timePreference;
    @Schema(description = "Own free-text wish; always null for the other side")
    private String message;
}
