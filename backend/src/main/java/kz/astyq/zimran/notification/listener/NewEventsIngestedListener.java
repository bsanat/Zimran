package kz.astyq.zimran.notification.listener;

import kz.astyq.zimran.event.model.domain.NewEventsIngestedEvent;
import kz.astyq.zimran.notification.service.NotificationMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Runs alert matching after every ingestion run that inserted new events. Matching failures never fail ingestion.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewEventsIngestedListener {

    private final NotificationMatchingService notificationMatchingService;

    @EventListener
    public void onNewEventsIngested(NewEventsIngestedEvent ingested) {
        try {
            int created = notificationMatchingService.match(ingested.getEvents());
            if (created > 0) {
                log.info("Alerts for {}: {} notifications created", ingested.getCity().getId(), created);
            }
        } catch (RuntimeException e) {
            log.warn("Alert matching failed for {}: {}", ingested.getCity().getId(), e.toString());
        }
    }
}
