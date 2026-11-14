package kz.astyq.zimran.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * One page of {@code GET /api/events}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Page of events, sorted by start time then title")
public class EventPageViewResponse {

    private List<EventViewResponse> items;
    @Schema(description = "1-based page number", example = "1")
    private int page;
    @Schema(example = "20")
    private int pageSize;
    @Schema(description = "Number of matching events over all pages")
    private int total;
    @Schema(description = "Whether a next page has events")
    private boolean hasMore;
}
