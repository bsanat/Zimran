package kz.astyq.zimran.assistant.model.dto;

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
@Schema(description = "A way to loosen the filters")
public class RelaxationSuggestionViewResponse {

    @Schema(description = "increase_budget, widen_dates or more_interests", example = "widen_dates")
    private String code;
    @Schema(description = "Text in the guest's language", example = "Try a wider date range.")
    private String message;
}
