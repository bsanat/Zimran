package kz.astyq.zimran.planner.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.astyq.zimran.event.model.dto.EventViewResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "One recommended event")
public class RecommendationViewResponse {

    private EventViewResponse event;
    @Schema(description = "Integer 0..100", example = "87")
    private int matchScore;
    @Schema(description = "Why the event fits, in the guest's language")
    private String reason;
}
