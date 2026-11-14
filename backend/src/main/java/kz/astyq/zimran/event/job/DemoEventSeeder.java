package kz.astyq.zimran.event.job;

import kz.astyq.zimran.event.service.DemoEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds the fictional sample events at startup, only when {@code app.demo.enabled=true}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
public class DemoEventSeeder implements ApplicationRunner {

    private final DemoEventService demoEventService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Demo catalog seeded: {} sample events", demoEventService.seed());
    }
}
