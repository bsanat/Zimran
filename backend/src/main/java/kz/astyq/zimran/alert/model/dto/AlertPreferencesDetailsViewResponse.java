package kz.astyq.zimran.alert.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body of {@code GET /api/alerts/preferences}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "The caller's alert preferences, or the defaults")
public class AlertPreferencesDetailsViewResponse {

    private AlertPreferencesViewResponse preferences;
    @Schema(description = "True when the caller is not Premium: saving is refused and no alerts are sent")
    private boolean premiumRequired;
    @Schema(description = "False when nothing was saved yet (preferences are the defaults)")
    private boolean configured;
}
