package kz.astyq.zimran.core.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Warns once at startup about missing external API keys. The app still boots so the rest of the
 * API (and local development) keeps working; calls to the affected provider will fail at runtime.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalApiKeysCheck {

    private final TicketmasterProperties ticketmasterProperties;
    private final OpenAiProperties openAiProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void warnAboutMissingKeys() {
        if (!ticketmasterProperties.hasApiKey()) {
            log.warn("TICKETMASTER_API_KEY is not set: event search will be unavailable");
        }
        if (!openAiProperties.hasApiKey()) {
            log.warn("OPENAI_API_KEY is not set: AI ranking will be unavailable");
        }
    }
}
