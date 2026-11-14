package kz.astyq.zimran.pair.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body of {@code POST /api/pair/sessions}: the city and the creator's (side A) wishes.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PairSessionCreateRequest {

    @Schema(description = "Supported city, by id or name", example = "nyc", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 100)
    private String city;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull
    private PairPreferencesRequest participantA;
}
