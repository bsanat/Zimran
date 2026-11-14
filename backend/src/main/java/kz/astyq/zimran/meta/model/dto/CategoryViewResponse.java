package kz.astyq.zimran.meta.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Shared event category")
public class CategoryViewResponse {

    @Schema(description = "Category slug, the value of an event's categorySlug", example = "food_drinks")
    private String id;
    @Schema(description = "Label in the requested language", example = "Food & Drinks")
    private String label;
    @Schema(description = "Interest ids that belong to the category", example = "[\"food\"]")
    private List<String> interests;
}
