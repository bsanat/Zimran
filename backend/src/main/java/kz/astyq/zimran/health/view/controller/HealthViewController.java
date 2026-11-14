package kz.astyq.zimran.health.view.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.astyq.zimran.health.model.dto.HealthViewResponse;
import kz.astyq.zimran.health.view.service.HealthViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/health", produces = MediaType.APPLICATION_JSON_VALUE)
public class HealthViewController {

    private final HealthViewService healthViewService;

    @Operation(summary = "Liveness check")
    @GetMapping
    public ResponseEntity<HealthViewResponse> getHealth() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(healthViewService.getHealth());
    }
}
