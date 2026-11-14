package kz.astyq.zimran.planner.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Body of {@code POST /api/plan}, exactly the frontend's {@code PlannerResponse}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A plan of up to 5 recommended events")
public class PlanViewResponse {

    @Schema(description = "Random UUID", example = "3f2b8c1e-1d1a-4f7e-9c55-0b5c2a7e9d10")
    private String planId;
    @Schema(description = "Plan summary in the guest's language")
    private String summary;
    @Schema(description = "0..5 distinct scheduled events, best first")
    private List<RecommendationViewResponse> recommendations;
    @Schema(description = "Tips in the guest's language; may be empty")
    private List<String> suggestions;
}
