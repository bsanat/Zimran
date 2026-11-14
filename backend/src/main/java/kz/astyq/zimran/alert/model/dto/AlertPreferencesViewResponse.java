package kz.astyq.zimran.alert.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.astyq.zimran.alert.model.enums.AlertChannel;
import kz.astyq.zimran.alert.model.enums.AlertFrequency;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Alert preferences")
public class AlertPreferencesViewResponse {

    @Schema(description = "Supported city id", example = "nyc")
    private String city;
    @Schema(example = "[\"music\"]")
    private List<CategorySlug> categorySlugs;
    @Schema(nullable = true)
    private BigDecimal budgetMax;
    private AlertFrequency frequency;
    @Schema(example = "[\"in_app\"]")
    private List<AlertChannel> channels;
    private boolean paused;
    @Schema(nullable = true, description = "When alerts were first enabled or last un-paused; only events added "
            + "since then are notified")
    private Instant enabledAt;
}
