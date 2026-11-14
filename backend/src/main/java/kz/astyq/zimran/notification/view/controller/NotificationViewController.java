package kz.astyq.zimran.notification.view.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.astyq.zimran.core.configuration.OpenApiConfig;
import kz.astyq.zimran.core.exception.dto.ErrorResponse;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.web.CurrentScope;
import kz.astyq.zimran.notification.model.dto.NotificationPageViewResponse;
import kz.astyq.zimran.notification.model.dto.NotificationSearchRequest;
import kz.astyq.zimran.notification.view.service.NotificationViewService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notifications", description = "In-app notification center")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
public class NotificationViewController {

    private final NotificationViewService notificationViewService;

    @Operation(summary = "The caller's notifications",
            description = "Delivered notifications only (DAILY / WEEKLY alerts appear at their delivery time), "
                    + "newest first.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "One page of notifications")
    @ApiResponse(responseCode = "400", description = "Invalid page or size",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public ResponseEntity<NotificationPageViewResponse> getPage(@Valid @ParameterObject NotificationSearchRequest request,
                                                                @CurrentScope RequestScope scope) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(notificationViewService.getPage(request, scope));
    }

    @Operation(summary = "Mark one notification as read", description = "Idempotent.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "204", description = "Read")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown notification, or not the caller's",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable String id, @CurrentScope RequestScope scope) {
        notificationViewService.markRead(id, scope);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark every notification as read",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "204", description = "All read")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@CurrentScope RequestScope scope) {
        notificationViewService.markAllRead(scope);
        return ResponseEntity.noContent().build();
    }
}
