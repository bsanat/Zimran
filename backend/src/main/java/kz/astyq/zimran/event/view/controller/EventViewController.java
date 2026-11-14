package kz.astyq.zimran.event.view.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.astyq.zimran.core.exception.dto.ErrorResponse;
import kz.astyq.zimran.event.model.dto.EventDetailsViewResponse;
import kz.astyq.zimran.event.model.dto.EventPageViewResponse;
import kz.astyq.zimran.event.model.dto.EventSearchRequest;
import kz.astyq.zimran.event.view.service.EventViewService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@Tag(name = "Events", description = "Public catalog of real events (no login)")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/events", produces = MediaType.APPLICATION_JSON_VALUE)
public class EventViewController {

    private static final CacheControl CACHE_CONTROL = CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic();

    private final EventViewService eventViewService;

    @Operation(summary = "Search upcoming events",
            description = "Events in a supported city, optionally on one day, in one interest, matching a keyword "
                    + "or major only. Sorted by start time, then title; 10 per page.")
    @ApiResponse(responseCode = "200", description = "One page of events")
    @ApiResponse(responseCode = "400", description = "Invalid parameters, past date or unsupported city",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Event source unavailable",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public ResponseEntity<EventPageViewResponse> getPage(@Valid @ParameterObject EventSearchRequest request) {
        return ResponseEntity.ok().cacheControl(CACHE_CONTROL).body(eventViewService.getPage(request));
    }

    @Operation(summary = "Get one event")
    @ApiResponse(responseCode = "200", description = "The event")
    @ApiResponse(responseCode = "404", description = "Unknown or malformed event id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Event source unavailable",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<EventDetailsViewResponse> getById(
            @Parameter(description = "Public event id, URL-encoded", example = "json:evt-123")
            @PathVariable String id) {
        return ResponseEntity.ok().cacheControl(CACHE_CONTROL).body(eventViewService.getById(id));
    }
}
