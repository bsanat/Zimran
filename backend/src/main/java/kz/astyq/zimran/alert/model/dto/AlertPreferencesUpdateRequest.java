package kz.astyq.zimran.alert.model.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.astyq.zimran.alert.model.enums.AlertChannel;
import kz.astyq.zimran.alert.model.enums.AlertFrequency;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Body of {@code PUT /api/alerts/preferences}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertPreferencesUpdateRequest {

    public static final int MAX_CATEGORIES = 20;
    public static final int MAX_CHANNELS = 5;

    @Schema(description = "Supported city, by id or name", example = "nyc", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 100)
    private String city;

    @ArraySchema(arraySchema = @Schema(description = "At least one category slug (see GET /api/meta)",
            requiredMode = Schema.RequiredMode.REQUIRED),
            schema = @Schema(type = "string", example = "music"))
    @NotEmpty
    @Size(max = MAX_CATEGORIES)
    private List<@NotNull CategorySlug> categorySlugs;

    @Schema(nullable = true, description = "Highest acceptable lowest ticket price in USD; events with an unknown "
            + "price never match a set budget", example = "50")
    @DecimalMin("0")
    @Digits(integer = 7, fraction = 2)
    private BigDecimal budgetMax;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "DAILY")
    @NotNull
    private AlertFrequency frequency;

    @ArraySchema(arraySchema = @Schema(description = "Default [\"in_app\"]; \"email\" is not available yet (422)"),
            schema = @Schema(type = "string", allowableValues = {"in_app", "email"}))
    @Size(min = 1, max = MAX_CHANNELS)
    private List<@NotNull AlertChannel> channels;

    @Schema(description = "Default false")
    private Boolean paused;
}
