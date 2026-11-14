package kz.astyq.zimran.guest.view.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import kz.astyq.zimran.core.exception.dto.ErrorResponse;
import kz.astyq.zimran.guest.model.dto.GuestSessionViewResponse;
import kz.astyq.zimran.guest.view.service.GuestViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Guest", description = "Anonymous guest identity")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/guest", produces = MediaType.APPLICATION_JSON_VALUE)
public class GuestViewController {

    private final GuestViewService guestViewService;

    @Operation(summary = "Start a guest session",
            description = "Creates a new, independent guest (every call is a new guest) and returns a signed token. "
                    + "Send it as the X-Guest-Token header on endpoints that need a guest. No auth required.")
    @ApiResponse(responseCode = "201", description = "The new guest and its token")
    @ApiResponse(responseCode = "429", description = "Too many sessions from this client",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/session")
    public ResponseEntity<GuestSessionViewResponse> createSession(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(guestViewService.createSession(request.getRemoteAddr()));
    }
}
