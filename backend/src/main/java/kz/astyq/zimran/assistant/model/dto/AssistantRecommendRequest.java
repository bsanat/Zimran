package kz.astyq.zimran.assistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
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
 * Body of {@code POST /api/ai/recommend}: the (possibly edited) filters from {@code POST /api/ai/parse} plus the
 * original message. Business rules are checked by the server, so the filters may arrive null here.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantRecommendRequest {

    @Schema(description = "The guest's original message; helps the AI rank (e.g. \"jazz\")", maxLength = 500)
    @Size(max = 500)
    private String message;

    @Schema(description = "Supported city, by id or name", example = "nyc", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 100)
    private String city;

    @Schema(description = "At least one category slug; unknown ones are dropped", example = "[\"music\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 20)
    private List<@Size(max = 50) String> interests;

    @Schema(description = "First day, inclusive; today or later in the city", type = "string", format = "date",
            example = "2026-11-21", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate dateFrom;

    @Schema(description = "Last day, inclusive; default dateFrom. At most 7 days in total", type = "string",
            format = "date", example = "2026-11-21")
    private LocalDate dateTo;

    @Schema(description = "any, morning, afternoon or evening; default any")
    private TimePreference timeOfDay;

    @Schema(description = "Max price per ticket in USD, >= 0; absent = no limit, 0 = free only. With a budget, "
            + "events with an unknown price are excluded", example = "60")
    private BigDecimal budgetMax;

    @Schema(description = "Other wishes", example = "cozy places", maxLength = 500)
    @Size(max = 500)
    private String notes;
}
