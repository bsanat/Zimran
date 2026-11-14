package kz.astyq.zimran.event.model.dto;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import kz.astyq.zimran.event.model.enums.Interest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * Query of {@code GET /api/events}. Dates are {@code YYYY-MM-DD} in the city's timezone.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSearchRequest {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;

    @NotBlank
    @Parameter(description = "Supported city, by name or id", example = "New York", required = true)
    private String city;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Single day; shorthand for startDate = endDate = date (wins over both)",
            schema = @Schema(type = "string", format = "date", example = "2026-11-20"))
    private LocalDate date;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "First day, inclusive; default today in the city",
            schema = @Schema(type = "string", format = "date"))
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Last day, inclusive; default startDate. At most 7 days in total",
            schema = @Schema(type = "string", format = "date"))
    private LocalDate endDate;

    @Parameter(description = "Interests to include (repeat the parameter or send one comma-separated value, not both); empty = all")
    private List<Interest> category;

    @Min(1)
    @Parameter(description = "1-based page number", schema = @Schema(type = "integer", defaultValue = "1", minimum = "1"))
    private Integer page;

    @Min(1)
    @Max(50)
    @Parameter(description = "Events per page",
            schema = @Schema(type = "integer", defaultValue = "20", minimum = "1", maximum = "50"))
    private Integer pageSize;
}
