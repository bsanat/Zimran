package kz.astyq.zimran.demo.view.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.astyq.zimran.core.configuration.OpenApiConfig;
import kz.astyq.zimran.core.exception.dto.ErrorResponse;
import kz.astyq.zimran.core.model.domain.RequestScope;
import jakarta.validation.Valid;
import kz.astyq.zimran.core.web.CurrentScope;
import kz.astyq.zimran.demo.model.dto.DemoAlertCheckViewResponse;
import kz.astyq.zimran.demo.model.dto.DemoEventCreateRequest;
import kz.astyq.zimran.demo.view.service.DemoViewService;
import kz.astyq.zimran.event.model.dto.EventDetailsViewResponse;
import kz.astyq.zimran.subscription.model.dto.SubscriptionViewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Demo", description = "Demo-only features (app.demo.enabled)")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/demo", produces = MediaType.APPLICATION_JSON_VALUE)
public class DemoViewController {

    private final DemoViewService demoViewService;

    @Operation(summary = "Try Premium Demo",
            description = "Gives the caller DEMO_PREMIUM (demoOnly = true) without payment. Idempotent. "
                    + "Returns the updated subscription.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "The updated subscription")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Demo features are disabled",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/premium")
    public ResponseEntity<SubscriptionViewResponse> activatePremium(@CurrentScope RequestScope scope) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(demoViewService.activatePremium(scope));
    }

    @Operation(summary = "Add a sample event",
            description = "Adds one fictional event (source demo, isDemo = true) to the catalog as a new event, to "
                    + "try alerts. Caller must have DEMO_PREMIUM. Rate limited.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "The new sample event")
    @ApiResponse(responseCode = "400", description = "Invalid body, unsupported city or start not in the future",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Not DEMO_PREMIUM (DEMO_PREMIUM_REQUIRED)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Demo features are disabled",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "429", description = "Too many requests",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/events", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventDetailsViewResponse> createEvent(@Valid @RequestBody DemoEventCreateRequest request,
                                                                @CurrentScope RequestScope scope) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(demoViewService.createEvent(request, scope));
    }

    @Operation(summary = "Check alerts now",
            description = "Runs the real alert matching for the caller over sample events not yet notified to them. "
                    + "Demo-only override: matches are delivered immediately whatever the alert frequency. Running "
                    + "it again never notifies the same event twice. Caller must have DEMO_PREMIUM. Rate limited.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "What was checked and created")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Not DEMO_PREMIUM (DEMO_PREMIUM_REQUIRED)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Demo features are disabled",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "429", description = "Too many requests",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/check-alerts")
    public ResponseEntity<DemoAlertCheckViewResponse> checkAlerts(@CurrentScope RequestScope scope) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(demoViewService.checkAlerts(scope));
    }
}
