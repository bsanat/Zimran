package kz.astyq.zimran.assistant.model.dto;

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
@Schema(description = "One recommended event; every event field comes from the catalog, never from the AI")
public class AssistantRecommendationViewResponse {

    @Schema(example = "ticketmaster:abc123")
    private String eventId;
    @Schema(description = "Integer 0..100", example = "91")
    private int matchScore;
    @Schema(description = "Why the event fits, in the guest's language")
    private String reason;
    private EventViewResponse event;
}
