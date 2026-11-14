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
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import kz.astyq.zimran.moderation.model.dto.ReportCreateRequest;
import kz.astyq.zimran.moderation.view.service.ModerationViewService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Moderation", description = "Reports and blocks, free for every guest")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/reports", produces = MediaType.APPLICATION_JSON_VALUE)
public class ReportViewController {

    private final ModerationViewService moderationViewService;

    @Operation(summary = "Report a Buddy post, request or person",
            description = "Stored for moderation; the reported person is never told who reported them.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "204", description = "Reported")
    @ApiResponse(responseCode = "400", description = "Invalid body, or the caller's own item", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown post or request", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "429", description = "Too many reports", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> report(@Valid @RequestBody ReportCreateRequest request, @CurrentScope RequestScope scope) {
        moderationViewService.report(request, scope);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
