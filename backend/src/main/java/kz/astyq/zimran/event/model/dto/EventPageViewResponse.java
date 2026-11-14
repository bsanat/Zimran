package kz.astyq.zimran.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Body of {@code GET /api/events}: {@code {"events": [...], "pagination": {...}}}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Page of events, sorted by start time then title")
public class EventPageViewResponse {

    private List<EventViewResponse> events;
    private PaginationViewResponse pagination;
}
