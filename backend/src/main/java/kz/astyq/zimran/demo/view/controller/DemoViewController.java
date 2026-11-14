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
import kz.astyq.zimran.core.web.CurrentScope;
import kz.astyq.zimran.demo.view.service.DemoViewService;
import kz.astyq.zimran.subscription.model.dto.SubscriptionViewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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
}
