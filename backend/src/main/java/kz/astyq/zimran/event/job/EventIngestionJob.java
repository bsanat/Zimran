package kz.astyq.zimran.event.job;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.event.service.CatalogRefreshService;
import kz.astyq.zimran.event.service.EventIngestionService;
import kz.astyq.zimran.event.util.FailureDescriptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ingests every supported city on {@code app.ingestion.cron}, then expires past events. A city whose providers
 * fail is logged (no secrets) and skipped; the job itself never fails.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.ingestion", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EventIngestionJob {

    private final SupportedCityService supportedCityService;
    private final CatalogRefreshService catalogRefreshService;
    private final EventIngestionService eventIngestionService;

    @Scheduled(cron = "${app.ingestion.cron:0 */15 * * * *}")
    public void run() {
        int inserted = 0;
        int failedCities = 0;
        for (SupportedCity city : supportedCityService.getAll()) {
            try {
                inserted += catalogRefreshService.refresh(city).size();
            } catch (RuntimeException e) {
                failedCities++;
                log.warn("Event ingestion skipped city {}: {}", city.getId(), FailureDescriptions.describe(e));
            }
        }
        int expired = 0;
        try {
            expired = eventIngestionService.expirePastEvents();
        } catch (RuntimeException e) {
            log.warn("Expiring past events failed: {}", FailureDescriptions.describe(e));
        }
        log.info("Event ingestion done: {} new events, {} expired, {} cities failed", inserted, expired, failedCities);
    }
}
