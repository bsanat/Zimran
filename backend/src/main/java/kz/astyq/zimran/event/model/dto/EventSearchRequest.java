package kz.astyq.zimran.event.model.dto;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import kz.astyq.zimran.event.model.enums.Interest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Query of {@code GET /api/events}. {@code date} is {@code YYYY-MM-DD} in the city's timezone.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSearchRequest {

    public static final int DEFAULT_PAGE = 1;
    public static final int PAGE_SIZE = 10;
    public static final int MAX_PAGE = 100;
    public static final int MAX_KEYWORD_LENGTH = 100;

    @NotBlank
    @Parameter(description = "Supported city", example = "New York", required = true)
    private String city;

    @Parameter(description = "State code of the city", example = "NY")
    private String state;

    @Min(1)
    @Max(MAX_PAGE)
    @Parameter(description = "1-based page number; 10 events per page",
            schema = @Schema(type = "integer", defaultValue = "1", minimum = "1", maximum = "100"))
    private Integer page;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Single day, today or later; default: every upcoming event",
            schema = @Schema(type = "string", format = "date", example = "2026-11-20"))
    private LocalDate date;

    @Parameter(description = "Interest id", schema = @Schema(type = "string", example = "music"))
    private Interest category;

    @Parameter(description = "Shared category slug (see GET /api/meta)",
            schema = @Schema(type = "string", example = "food_drinks"))
    private CategorySlug categorySlug;

    @DecimalMin("0")
    @Parameter(description = "Highest acceptable lowest ticket price in USD; events with an unknown price are excluded",
            schema = @Schema(type = "number", example = "30"))
    private BigDecimal budgetMax;

    @Pattern(regexp = "true|false")
    @Parameter(description = "true = only events the source says are free; unknown prices are excluded",
            schema = @Schema(type = "string", allowableValues = {"true", "false"}))
    private String freeOnly;

    @Size(max = MAX_KEYWORD_LENGTH)
    @Parameter(description = "Case-insensitive text in the title, description or venue")
    private String keyword;

    @Pattern(regexp = "true|false")
    @Parameter(description = "true = only major events", schema = @Schema(type = "string", allowableValues = {"true", "false"}))
    private String major;
}
