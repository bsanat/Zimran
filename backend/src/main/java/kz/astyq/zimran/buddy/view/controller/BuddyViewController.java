package kz.astyq.zimran.buddy.view.controller;

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
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kz.astyq.zimran.buddy.model.dto.BuddyDecisionRequest;
import kz.astyq.zimran.buddy.model.dto.BuddyIncomingRequestViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyMeViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyOutgoingRequestViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyPostCreateRequest;
import kz.astyq.zimran.buddy.model.dto.BuddyPostListViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyPostViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyRequestCreateRequest;
import kz.astyq.zimran.buddy.view.service.BuddyViewService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Event Buddy", description = "Find company for a real event; no contact data is ever exchanged")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/buddy", produces = MediaType.APPLICATION_JSON_VALUE)
public class BuddyViewController {

    private final BuddyViewService buddyViewService;

    @Operation(summary = "Open posts for an event",
            description = "Public. Only OPEN posts whose event has not started; with a guest session, posts of people "
                    + "the caller blocked or was blocked by are hidden. Never contains scope ids or contact data.")
    @ApiResponse(responseCode = "200", description = "The posts, newest first")
    @GetMapping("/posts")
    public ResponseEntity<BuddyPostListViewResponse> openPosts(
            @Parameter(description = "Public event id", example = "json:ny-prospect-park-chess")
            @RequestParam @NotBlank @Size(max = 300) String eventId,
            @CurrentScope(required = false) RequestScope scope) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(buddyViewService.openPosts(eventId, scope));
    }

    @Operation(summary = "My posts and requests",
            description = "My latest posts with the requests they received, and my latest requests with their posts. "
                    + "Accepted requests carry a safety notice.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "My Buddy activity")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/me")
    public ResponseEntity<BuddyMeViewResponse> me(@CurrentScope RequestScope scope) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(buddyViewService.me(scope));
    }

    @Operation(summary = "Create a post",
            description = "Premium or demo Premium; ageConfirmed must be true. The event must exist and be upcoming; "
                    + "one OPEN post per person and event. Contact details in the text are rejected. Rate limited.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "The new OPEN post")
    @ApiResponse(responseCode = "400", description = "Invalid body, contact details, or the event is not upcoming", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Not Premium (PREMIUM_REQUIRED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown event", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "An OPEN post for this event already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "ageConfirmed is not true (AGE_CONFIRMATION_REQUIRED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "429", description = "Too many requests", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/posts", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BuddyPostViewResponse createPost(@Valid @RequestBody BuddyPostCreateRequest request,
                                            @CurrentScope RequestScope scope) {
        return buddyViewService.createPost(request, scope);
    }

    @Operation(summary = "Request to join a post",
            description = "Premium or demo Premium; ageConfirmed must be true. Not the caller's own post, the post must be "
                    + "OPEN and neither side may have blocked the other. One request per person and post.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "The PENDING request")
    @ApiResponse(responseCode = "400", description = "Invalid body or contact details", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Not Premium (PREMIUM_REQUIRED), or blocked (FORBIDDEN)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown post", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Own post, post not open, or already requested", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "ageConfirmed is not true (AGE_CONFIRMATION_REQUIRED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "429", description = "Too many requests", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/posts/{id}/requests", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BuddyOutgoingRequestViewResponse createRequest(@PathVariable String id,
                                                          @Valid @RequestBody BuddyRequestCreateRequest request,
                                                          @CurrentScope RequestScope scope) {
        return buddyViewService.createRequest(id, request, scope);
    }

    @Operation(summary = "Accept or decline a request",
            description = "Post owner only. ACCEPT matches the post and declines its other pending requests.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "The decided request")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Not the post owner (FORBIDDEN)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown request", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Request not pending, or post not open", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/requests/{id}/decision", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BuddyIncomingRequestViewResponse decide(@PathVariable String id, @Valid @RequestBody BuddyDecisionRequest request,
                                                   @CurrentScope RequestScope scope) {
        return buddyViewService.decide(id, request, scope);
    }

    @Operation(summary = "Cancel my request",
            description = "Sender only. Cancelling an accepted request reopens the post while the event has not started.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "The cancelled request")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Not the sender (FORBIDDEN)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown request", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Request already ended", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/requests/{id}/cancel")
    public BuddyOutgoingRequestViewResponse cancel(@PathVariable String id, @CurrentScope RequestScope scope) {
        return buddyViewService.cancel(id, scope);
    }

    @Operation(summary = "Close my post",
            description = "Owner only. An OPEN or MATCHED post becomes CLOSED; its pending requests are declined.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "The closed post")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Not the owner (FORBIDDEN)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown post", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Post already ended", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/posts/{id}/close")
    public BuddyPostViewResponse close(@PathVariable String id, @CurrentScope RequestScope scope) {
        return buddyViewService.close(id, scope);
    }
}
