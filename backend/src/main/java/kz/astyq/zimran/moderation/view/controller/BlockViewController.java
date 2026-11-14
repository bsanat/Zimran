package kz.astyq.zimran.moderation.view.controller;

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
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import kz.astyq.zimran.moderation.model.dto.BlockCreateRequest;
import kz.astyq.zimran.moderation.model.dto.BlockViewResponse;
import kz.astyq.zimran.moderation.view.service.ModerationViewService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Moderation", description = "Reports and blocks, free for every guest")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/blocks", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlockViewController {

    private final ModerationViewService moderationViewService;

    @Operation(summary = "Block the author of a Buddy post or request",
            description = "Works both ways: neither side sees the other's Buddy posts or can send requests, and open "
                    + "requests between them are cancelled. Idempotent. Returns a scopeRef to lift the block.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "Blocked")
    @ApiResponse(responseCode = "400", description = "Invalid body, or the caller's own item", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown post or request", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public BlockViewResponse block(@Valid @RequestBody BlockCreateRequest request, @CurrentScope RequestScope scope) {
        return moderationViewService.block(request, scope);
    }

    @Operation(summary = "Lift a block", security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "204", description = "Unblocked")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown block, or another guest's", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{scopeRef}")
    public ResponseEntity<Void> unblock(@PathVariable String scopeRef, @CurrentScope RequestScope scope) {
        moderationViewService.unblock(scopeRef, scope);
        return ResponseEntity.noContent().build();
    }
}
