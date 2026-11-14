package kz.astyq.zimran.planner.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
 * Body of {@code POST /api/plan}. Dates are {@code YYYY-MM-DD} in the city's timezone. Business rules are
 * checked by {@code PlanRequestValidationService}, so every field may arrive null here.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanCreateRequest {

    @Schema(description = "Supported city, by name or id", example = "New York", requiredMode = Schema.RequiredMode.REQUIRED)
    private String city;

    @Schema(description = "State code of the city; needed only when two cities share a name (e.g. Portland)",
            example = "NY")
    private String state;

    @Schema(description = "At least one interest id (e.g. music); duplicates are ignored", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Interest> interests;

    @Schema(description = "First day, inclusive; today or later", type = "string", format = "date",
            example = "2026-11-20", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate startDate;

    @Schema(description = "Last day, inclusive; default startDate. At most 7 days in total", type = "string",
            format = "date")
    private LocalDate endDate;

    @Schema(description = "Maximum price per ticket in USD, >= 0; absent = no limit, "
            + "0 = free events only", example = "50")
    private BigDecimal budgetMax;

    @Schema(description = "Preferred time of day (any, morning, afternoon, evening); default any")
    private TimePreference timePreference;

    @Schema(description = "Free-text wishes, at most 500 characters")
    private String notes;
}
