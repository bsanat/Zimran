package kz.astyq.zimran.subscription.view.controller;

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
import kz.astyq.zimran.subscription.model.dto.SubscriptionViewResponse;
import kz.astyq.zimran.subscription.view.service.SubscriptionViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Subscription", description = "Premium status, plan and free-tier limits")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/subscription", produces = MediaType.APPLICATION_JSON_VALUE)
public class SubscriptionViewController {

    private final SubscriptionViewService subscriptionViewService;

    @Operation(summary = "Get the caller's subscription",
            description = "Status (FREE, DEMO_PREMIUM, PREMIUM_ACTIVE), the Premium plan and uses left in the "
                    + "current window. Feature labels follow the outly-lang cookie or Accept-Language.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "The subscription")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public ResponseEntity<SubscriptionViewResponse> getSubscription(@CurrentScope RequestScope scope) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(subscriptionViewService.getSubscription(scope));
    }
}
