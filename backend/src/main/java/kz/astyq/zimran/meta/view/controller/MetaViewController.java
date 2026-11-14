package kz.astyq.zimran.meta.view.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.astyq.zimran.meta.model.dto.MetaViewResponse;
import kz.astyq.zimran.meta.view.service.MetaViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@Tag(name = "Meta", description = "Reference data (no login)")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/meta", produces = MediaType.APPLICATION_JSON_VALUE)
public class MetaViewController {

    private static final CacheControl CACHE_CONTROL = CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic();

    private final MetaViewService metaViewService;

    @Operation(summary = "Reference data",
            description = "Event categories (the shared vocabulary of catalog, AI and alerts) with labels in the "
                    + "English.")
    @ApiResponse(responseCode = "200", description = "Reference data")
    @GetMapping
    public ResponseEntity<MetaViewResponse> getMeta() {
        return ResponseEntity.ok()
                .cacheControl(CACHE_CONTROL)
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE, HttpHeaders.COOKIE)
                .body(metaViewService.getMeta());
    }
}
