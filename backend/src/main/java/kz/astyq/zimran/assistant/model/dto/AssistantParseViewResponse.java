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
 * Body of {@code POST /api/ai/parse}: the filters read from the message, each validated by the server. Unknown
 * or invalid values are null (never guessed) and listed in {@code missingFields}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Search filters read from a guest message")
public class AssistantParseViewResponse {

    @Schema(description = "Supported city id; null when missing or not supported", example = "nyc")
    private String city;
    @Schema(description = "Category slugs (see GET /api/meta); unknown ones are dropped", example = "[\"music\"]")
    private List<String> interests;
    @Schema(description = "First day, YYYY-MM-DD in the city's timezone; today or later", type = "string",
            format = "date", example = "2026-11-21")
    private String dateFrom;
    @Schema(description = "Last day, YYYY-MM-DD; not before dateFrom", type = "string", format = "date",
            example = "2026-11-21")
    private String dateTo;
    @Schema(description = "any, morning, afternoon or evening; null when not said", example = "evening")
    private String timeOfDay;
    @Schema(description = "Max price per ticket in USD, >= 0; null = no limit", example = "60")
    private BigDecimal budgetMax;
    @Schema(description = "Other wishes from the message", example = "cozy places")
    private String notes;
    @Schema(description = "Fields to ask the guest about: city, interests, dateFrom, dateTo, timeOfDay, budgetMax")
    private List<String> missingFields;
    @Schema(description = "Follow-up question in the guest's language when city, dates or interests are missing")
    private String clarificationQuestion;
}
