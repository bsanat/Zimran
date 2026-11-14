package kz.astyq.zimran.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Position of a page")
public class PaginationViewResponse {

    @Schema(description = "1-based page number", example = "1")
    private int page;
    @Schema(example = "10")
    private int pageSize;
    @Schema(description = "Whether the next page has events")
    private boolean hasMore;
}
