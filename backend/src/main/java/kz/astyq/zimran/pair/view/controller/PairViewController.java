package kz.astyq.zimran.pair.view.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import kz.astyq.zimran.pair.model.dto.PairJoinCreateRequest;
import kz.astyq.zimran.pair.model.dto.PairJoinViewResponse;
import kz.astyq.zimran.pair.model.dto.PairSessionCreateRequest;
import kz.astyq.zimran.pair.model.dto.PairSessionCreateViewResponse;
import kz.astyq.zimran.pair.model.dto.PairSessionViewResponse;
import kz.astyq.zimran.pair.model.dto.PairVoteCreateRequest;
import kz.astyq.zimran.pair.view.service.PairViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Pair Planning", description = "Two people pick events together: both give their wishes, the server "
        + "suggests events that fit both, both vote")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/pair", produces = MediaType.APPLICATION_JSON_VALUE)
public class PairViewController {

    private final PairViewService pairViewService;

    @Operation(summary = "Create a session (side A)",
            description = "Uses the free Pair trial or the Premium fair-use quota. Returns a single-use invite link "
                    + "(valid app.pair.invite-ttl, default 48h) and the creator's participant token; both are "
                    + "shown only once.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "The session, WAITING_FOR_B")
    @ApiResponse(responseCode = "400", description = "Invalid wishes; details.field names the field",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Free Pair trial used up (FREE_LIMIT_REACHED)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "429", description = "Too many requests, or Premium fair-use limit reached",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/sessions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PairSessionCreateViewResponse> create(@Valid @RequestBody PairSessionCreateRequest request,
                                                                @CurrentScope RequestScope scope) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pairViewService.create(request, scope));
    }

    @Operation(summary = "Join a session with the invite (side B)",
            description = "The invite works once. Suggestions are computed now: status READY, or NO_MATCH when no "
                    + "event fits both sides (see GET for the conflict).",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "Joined")
    @ApiResponse(responseCode = "400", description = "Invalid wishes; details.field names the field",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown or revoked invite",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Already joined (PAIR_ALREADY_JOINED), or the creator's own "
            + "invite (CONFLICT)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "410", description = "Invite expired (PAIR_INVITE_EXPIRED)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/join", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PairJoinViewResponse> join(@Valid @RequestBody PairJoinCreateRequest request,
                                                     @CurrentScope RequestScope scope) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pairViewService.join(request, scope));
    }

    @Operation(summary = "The session as the caller sees it",
            description = "The other side's free-text message is never shown; the other side's vote on an event "
                    + "only after the caller voted on it.",
            security = @SecurityRequirement(name = OpenApiConfig.PAIR_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "The session")
    @ApiResponse(responseCode = "404", description = "Unknown session, or missing or wrong X-Pair-Token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{sessionId}")
    public ResponseEntity<PairSessionViewResponse> get(
            @PathVariable String sessionId,
            @Parameter(hidden = true) @RequestHeader(value = OpenApiConfig.PAIR_TOKEN_HEADER, required = false)
            String participantToken) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(pairViewService.get(sessionId, participantToken));
    }

    @Operation(summary = "Vote on a suggested event",
            description = "LIKE, MAYBE or PASS; a new vote replaces the previous one. The first event both like "
                    + "makes the session MATCHED; votes on every suggestion without a common LIKE make it NO_MATCH.",
            security = @SecurityRequirement(name = OpenApiConfig.PAIR_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "The updated session")
    @ApiResponse(responseCode = "400", description = "Not a suggested event (details.field=eventId)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown session, or missing or wrong X-Pair-Token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "The session does not accept votes (not READY)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/{sessionId}/votes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PairSessionViewResponse> vote(
            @PathVariable String sessionId,
            @Parameter(hidden = true) @RequestHeader(value = OpenApiConfig.PAIR_TOKEN_HEADER, required = false)
            String participantToken,
            @Valid @RequestBody PairVoteCreateRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(pairViewService.vote(sessionId, participantToken, request));
    }

    @Operation(summary = "Revoke the session (creator only)",
            security = @SecurityRequirement(name = OpenApiConfig.PAIR_TOKEN_SCHEME))
    @ApiResponse(responseCode = "204", description = "Revoked; the invite no longer works")
    @ApiResponse(responseCode = "403", description = "Not the creator",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown session, or missing or wrong X-Pair-Token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "The session already ended",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{sessionId}/revoke")
    public ResponseEntity<Void> revoke(
            @PathVariable String sessionId,
            @Parameter(hidden = true) @RequestHeader(value = OpenApiConfig.PAIR_TOKEN_HEADER, required = false)
            String participantToken) {
        pairViewService.revoke(sessionId, participantToken);
        return ResponseEntity.noContent().build();
    }
}
