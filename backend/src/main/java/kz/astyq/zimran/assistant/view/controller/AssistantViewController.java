package kz.astyq.zimran.assistant.view.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kz.astyq.zimran.assistant.model.dto.AssistantChatRequest;
import kz.astyq.zimran.assistant.model.dto.AssistantChatViewResponse;
import kz.astyq.zimran.assistant.model.dto.AssistantParseRequest;
import kz.astyq.zimran.assistant.model.dto.AssistantParseViewResponse;
import kz.astyq.zimran.assistant.model.dto.AssistantRecommendRequest;
import kz.astyq.zimran.assistant.model.dto.AssistantRecommendViewResponse;
import kz.astyq.zimran.assistant.view.service.AssistantViewService;
import kz.astyq.zimran.core.configuration.OpenApiConfig;
import kz.astyq.zimran.core.exception.dto.ErrorResponse;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.web.CurrentScope;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI assistant", description = "Free-text event search: message to filters, filters to AI-ranked events")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/ai", produces = MediaType.APPLICATION_JSON_VALUE)
public class AssistantViewController {

    private final AssistantViewService assistantViewService;

    @Operation(summary = "Read search filters from a message",
            description = "The AI only extracts; the server validates every field (supported city, known "
                    + "category slugs, dates from today in the city's timezone, budget >= 0). Invalid or missing "
                    + "values are null and listed in missingFields; clarificationQuestion follows the outly-lang "
                    + "cookie or Accept-Language. No guest session needed (X-Guest-Token is optional).")
    @ApiResponse(responseCode = "200", description = "The filters")
    @ApiResponse(responseCode = "400", description = "Blank or too long message",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "429", description = "Too many requests from this guest or client",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "AI unavailable (AI_UNAVAILABLE); no guessed result",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/parse", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssistantParseViewResponse> parse(@Valid @RequestBody AssistantParseRequest request,
                                                            @CurrentScope(required = false) RequestScope scope,
                                                            HttpServletRequest httpRequest) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(assistantViewService.parse(request, scope, httpRequest.getRemoteAddr()));
    }

    @Operation(summary = "Chat with the AI assistant (Gemini)",
            description = "One chat turn: send the new message plus the preferences and conversationId of the "
                    + "previous answer. The AI reads the message; the server validates and merges it. While city, "
                    + "interests or dates are missing the answer asks for them (clarifications, i18n texts). Then "
                    + "the AI ranks matching catalog events (result). The first result counts the assistant quota "
                    + "once and returns a conversationId; later results in that conversation are turns (limit "
                    + "app.ai.max-conversation-turns, open for app.ai.conversation-ttl). Needs a guest session.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "The chat answer")
    @ApiResponse(responseCode = "400", description = "Blank or too long message, or invalid preferences",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Free assistant quota used up (FREE_LIMIT_REACHED)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown conversation (ASSISTANT_CONVERSATION_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "410", description = "Conversation expired (ASSISTANT_CONVERSATION_EXPIRED)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "429", description = "Too many requests, or the conversation's turn limit "
            + "(ASSISTANT_TURN_LIMIT)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "AI or event source unavailable",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssistantChatViewResponse> chat(@Valid @RequestBody AssistantChatRequest request,
                                                          @CurrentScope RequestScope scope) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(assistantViewService.chat(request, scope));
    }

    @Operation(summary = "Recommend events for the filters",
            description = "Hard constraints are filtered in code; the AI ranks up to 5 of the remaining events "
                    + "and explains each pick. Event fields come from the catalog, never from the AI. No matching "
                    + "events: empty recommendations with relaxationSuggestions (no AI call, no quota use). Needs a "
                    + "guest session (POST /api/guest/session, no login) for the free quota / Premium fair use.",
            security = @SecurityRequirement(name = OpenApiConfig.GUEST_TOKEN_SCHEME))
    @ApiResponse(responseCode = "200", description = "The recommendations")
    @ApiResponse(responseCode = "400", description = "Invalid filters; details.field names the field",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired X-Guest-Token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Free assistant quota used up (FREE_LIMIT_REACHED)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "429", description = "Too many requests, or Premium fair-use limit reached",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "AI or event source unavailable",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/recommend", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssistantRecommendViewResponse> recommend(
            @Valid @RequestBody AssistantRecommendRequest request, @CurrentScope RequestScope scope) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(assistantViewService.recommend(request, scope));
    }
}
