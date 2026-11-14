package kz.astyq.zimran.notification.model.dto;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query of {@code GET /api/notifications}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSearchRequest {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_PAGE = 1000;
    public static final int MAX_SIZE = 50;

    @Min(1)
    @Max(MAX_PAGE)
    @Parameter(description = "1-based page number",
            schema = @Schema(type = "integer", defaultValue = "1", minimum = "1", maximum = "1000"))
    private Integer page;

    @Min(1)
    @Max(MAX_SIZE)
    @Parameter(description = "Notifications per page",
            schema = @Schema(type = "integer", defaultValue = "20", minimum = "1", maximum = "50"))
    private Integer size;

    public int pageOrDefault() {
        return page != null ? page : DEFAULT_PAGE;
    }

    public int sizeOrDefault() {
        return size != null ? size : DEFAULT_SIZE;
    }
}
