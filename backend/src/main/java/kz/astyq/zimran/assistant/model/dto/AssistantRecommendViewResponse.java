package kz.astyq.zimran.assistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Body of {@code POST /api/ai/recommend}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Up to 5 AI-ranked events")
public class AssistantRecommendViewResponse {

    @Schema(description = "Short summary in the guest's language", example = "A laid-back music evening")
    private String summary;
    @Schema(description = "0..5 distinct events, best first")
    private List<AssistantRecommendationViewResponse> recommendations;
    @Schema(description = "Ways to loosen the filters; only when there are no recommendations")
    private List<RelaxationSuggestionViewResponse> relaxationSuggestions;
}
