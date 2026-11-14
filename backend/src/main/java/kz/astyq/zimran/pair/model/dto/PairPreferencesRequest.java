package kz.astyq.zimran.pair.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import kz.astyq.zimran.event.model.enums.Interest;
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
 * One side's wishes. Business rules are checked like {@code POST /api/plan}, so fields may arrive null here.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PairPreferencesRequest {

    @Schema(description = "At least one interest id; duplicates are ignored", example = "[\"music\",\"food\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 20)
    private List<Interest> interests;

    @Schema(description = "First day, inclusive; today or later in the city", type = "string", format = "date",
            example = "2026-11-20", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate dateFrom;

    @Schema(description = "Last day, inclusive; default dateFrom. At most 7 days in total", type = "string",
            format = "date", example = "2026-11-22")
    private LocalDate dateTo;

    @Schema(description = "any, morning, afternoon or evening; default any")
    private TimePreference timePreference;

    @Schema(description = "Max price per ticket in USD, >= 0; absent = no limit", example = "60")
    private BigDecimal budgetMax;

    @Schema(description = "Free-text wish; never shown to the other side", maxLength = 500)
    @Size(max = 500)
    private String message;
}
