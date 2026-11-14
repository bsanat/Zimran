package kz.astyq.zimran.assistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
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
 * The preferences an assistant chat returned on its previous turn, sent back by the client. Every field is
 * optional; an unknown city is ignored.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantChatPreferencesRequest {

    @Schema(description = "Supported city id or name", example = "New York")
    @Size(max = 100)
    private String city;

    @Schema(description = "Planner interest ids", example = "[\"music\"]")
    @Size(max = 20)
    private List<@NotNull Interest> interests;

    @Schema(type = "string", format = "date", example = "2026-11-21")
    private LocalDate startDate;

    @Schema(type = "string", format = "date", example = "2026-11-21")
    private LocalDate endDate;

    @Schema(description = "Max price per ticket in USD, >= 0; absent = no limit", example = "60")
    @DecimalMin("0")
    @Digits(integer = 7, fraction = 2)
    private BigDecimal budgetMax;

    private TimePreference timePreference;

    @Size(max = 500)
    private String notes;
}
