package kz.astyq.zimran.planner.view.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.astyq.zimran.core.exception.dto.ErrorResponse;
import kz.astyq.zimran.planner.model.dto.PlanCreateRequest;
import kz.astyq.zimran.planner.model.dto.PlanViewResponse;
import kz.astyq.zimran.planner.view.service.PlannerViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Planner", description = "AI leisure plan for a guest (no login)")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/plan", produces = MediaType.APPLICATION_JSON_VALUE)
public class PlannerViewController {

    private final PlannerViewService plannerViewService;

    @Operation(summary = "Create a plan",
            description = "Up to 5 real events for the guest's interests, dates, time of day and budget, ranked by "
                    + "AI (rule-based when AI is unavailable). Texts are in English.")
    @ApiResponse(responseCode = "200", description = "The plan")
    @ApiResponse(responseCode = "400", description = "Invalid request; details.field names the field",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "413", description = "Body larger than 10 KB",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "429", description = "Too many plans from this client",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Event source unavailable",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PlanViewResponse> createPlan(@Valid @RequestBody PlanCreateRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(plannerViewService.createPlan(request));
    }
}
