package kz.astyq.zimran.alert.view.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.astyq.zimran.alert.model.dto.AlertPreferencesDetailsViewResponse;
import kz.astyq.zimran.alert.model.dto.AlertPreferencesSaveViewResponse;
import kz.astyq.zimran.alert.model.dto.AlertPreferencesUpdateRequest;
import kz.astyq.zimran.alert.view.service.AlertViewService;
import kz.astyq.zimran.core.configuration.OpenApiConfig;
import kz.astyq.zimran.core.exception.dto.ErrorResponse;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.web.CurrentScope;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Alerts", description = "Notify me about new events that match my preferences (Premium)")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/alerts", produces = MediaType.APPLICATION_JSON_VALUE)
public class AlertViewController {

    private final AlertViewService alertViewService;

    @Operation(summary = "The caller's alert preferences",
            description = "Defaults when nothing is saved (configured = false). Free callers get them too, with "
                    + "premiumRequired = true.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "The preferences")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/preferences")
    public ResponseEntity<AlertPreferencesDetailsViewResponse> getPreferences(@CurrentScope RequestScope scope) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(alertViewService.getPreferences(scope));
    }

    @Operation(summary = "Save alert preferences (Premium)",
            description = "Replaces the preferences. Enabling or un-pausing alerts starts them now: only events "
                    + "added to the catalog from then on are notified.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "Saved")
    @ApiResponse(responseCode = "400", description = "Invalid preferences; details.field names the field",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Not Premium (PREMIUM_REQUIRED)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "Channel not available yet, e.g. email (CHANNEL_NOT_AVAILABLE)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping(value = "/preferences", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AlertPreferencesSaveViewResponse> updatePreferences(
            @Valid @RequestBody AlertPreferencesUpdateRequest request, @CurrentScope RequestScope scope) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(alertViewService.updatePreferences(request, scope));
    }
}
