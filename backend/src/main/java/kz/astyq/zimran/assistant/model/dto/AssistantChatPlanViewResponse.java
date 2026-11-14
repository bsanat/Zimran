package kz.astyq.zimran.assistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * The events an assistant chat recommends once city, interests and dates are known.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantChatPlanViewResponse {

    @Schema(description = "The ranked plan's id; null when no event matched (no AI call)")
    private String planId;
    private String summary;
    @Schema(description = "At most 5 events, best first; event fields come from the catalog, only the reason "
            + "from the AI")
    private List<AssistantRecommendationViewResponse> recommendations;
    @Schema(description = "Ways to loosen the filters; only when nothing matched")
    private List<RelaxationSuggestionViewResponse> relaxationSuggestions;
}
