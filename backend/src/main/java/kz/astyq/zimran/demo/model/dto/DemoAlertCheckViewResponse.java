package kz.astyq.zimran.demo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Body of {@code POST /api/demo/check-alerts}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Outcome of the demo alert check")
public class DemoAlertCheckViewResponse {

    @Schema(description = "Demo events examined (not yet notified to the caller)")
    private int checked;
    @Schema(description = "Notifications created")
    private int created;
    @Schema(description = "Ids of the created notifications")
    private List<String> notificationIds;
}
