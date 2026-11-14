package kz.astyq.zimran.buddy.job;

import kz.astyq.zimran.buddy.service.BuddyExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Marks Buddy posts EXPIRED once their event has started ({@code app.buddy.expiry-cron}). Reads already treat
 * such posts as expired, so a late run only tidies the stored status; the job never fails.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BuddyExpiryJob {

    private final BuddyExpiryService buddyExpiryService;

    @Scheduled(cron = "${app.buddy.expiry-cron:0 */5 * * * *}")
    public void run() {
        try {
            int expired = buddyExpiryService.expireDue();
            if (expired > 0) {
                log.info("Buddy posts expired: {}", expired);
            }
        } catch (RuntimeException e) {
            log.warn("Expiring Buddy posts failed: {}", e.toString());
        }
    }
}
