package kz.astyq.zimran.demo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Body of {@code POST /api/demo/events}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoEventCreateRequest {

    public static final int MAX_TITLE_LENGTH = 200;

    @Schema(type = "string", example = "music", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private CategorySlug categorySlug;

    @Schema(nullable = true, description = "Supported city, by id or name; default: the pilot city", example = "nyc")
    @Size(max = 100)
    private String city;

    @Schema(nullable = true, description = "Default: \"Sample <category> event\"")
    @Size(max = MAX_TITLE_LENGTH)
    private String title;

    @Schema(nullable = true, description = "ISO-8601 with offset, in the future; default: tomorrow 19:00 city time",
            example = "2026-11-20T20:00:00-05:00")
    private OffsetDateTime startAt;

    @Schema(nullable = true, description = "Ticket price in USD; default: unknown price", example = "40")
    @DecimalMin("0")
    @Digits(integer = 7, fraction = 2)
    private BigDecimal priceMax;
}
