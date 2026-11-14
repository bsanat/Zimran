package kz.astyq.zimran.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body of {@code GET /api/events/{id}}: {@code {"event": {...}}}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "One event")
public class EventDetailsViewResponse {

    private EventViewResponse event;
}
