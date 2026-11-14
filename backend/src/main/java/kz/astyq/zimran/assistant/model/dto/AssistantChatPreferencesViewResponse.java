package kz.astyq.zimran.assistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * What the chat knows after this turn; null = not known yet (time of day: any, budget: no limit). Send it back
 * as {@code preferences} on the next turn.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantChatPreferencesViewResponse {

    @Schema(description = "Supported city name", example = "New York")
    private String city;
    @Schema(description = "Planner interest ids", example = "[\"music\"]")
    private List<String> interests;
    @Schema(type = "string", format = "date", example = "2026-11-21")
    private String startDate;
    @Schema(type = "string", format = "date", example = "2026-11-21")
    private String endDate;
    private BigDecimal budgetMax;
    @Schema(example = "evening")
    private String timePreference;
    private String notes;
}
