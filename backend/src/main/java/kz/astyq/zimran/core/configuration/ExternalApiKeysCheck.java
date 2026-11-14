package kz.astyq.zimran.core.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Warns once at startup about missing external settings. The app still boots so the rest of the API (and local
 * development) keeps working: without an event source the catalog answers 503, without a Gemini key the planner
 * ranks with its rule-based fallback.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalApiKeysCheck {

    private final EventSourceProperties eventSourceProperties;
    private final GeminiProperties geminiProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void warnAboutMissingKeys() {
        if (!eventSourceProperties.isConfigured()) {
            log.warn("EVENTS_JSON_SOURCE is not set: event search will be unavailable");
        }
        if (!geminiProperties.hasApiKey()) {
            log.warn("GEMINI_API_KEY is not set: plans use the rule-based ranking");
        }
    }
}
