package kz.astyq.zimran.alert.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body of {@code PUT /api/alerts/preferences}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Saved alert preferences")
public class AlertPreferencesSaveViewResponse {

    private boolean saved;
    private AlertPreferencesViewResponse preferences;
}
