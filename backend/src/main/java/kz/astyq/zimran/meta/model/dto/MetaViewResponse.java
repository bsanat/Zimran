package kz.astyq.zimran.meta.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Body of {@code GET /api/meta}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Reference data for the frontend")
public class MetaViewResponse {

    @Schema(description = "Every category, in a stable order")
    private List<CategoryViewResponse> categories;
}
