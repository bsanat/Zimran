package kz.astyq.zimran.event.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.configuration.DemoProperties;
import kz.astyq.zimran.core.configuration.IngestionProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.domain.NewEventsIngestedEvent;
import kz.astyq.zimran.event.repository.CatalogEventRepository;
import kz.astyq.zimran.event.service.CatalogRefreshService;
import kz.astyq.zimran.event.service.EventIngestionService;
import kz.astyq.zimran.event.service.EventProvider;
import kz.astyq.zimran.event.util.CatalogEventSpecifications;
import kz.astyq.zimran.event.util.EventFilter;
import kz.astyq.zimran.event.util.FailureDescriptions;
import kz.astyq.zimran.event.util.MessageCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogRefreshServiceImpl implements CatalogRefreshService {

    private final List<EventProvider> eventProviders;
    private final EventIngestionService eventIngestionService;
    private final CatalogEventRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final IngestionProperties ingestionProperties;
    private final DemoProperties demoProperties;
    private final SupportedCityService supportedCityService;
    private final Clock clock;

    /** City id -> when its last live fetch started. */
    private final Map<String, Instant> liveFetches = new ConcurrentHashMap<>();

    @Override
    public List<Event> refresh(SupportedCity city) {
        Objects.requireNonNull(city, "city");
        List<Event> events = new ArrayList<>();
        RuntimeException failure = null;
        int succeeded = 0;
        for (EventProvider provider : eventProviders) {
            try {
                provider.getAll().stream()
                        .filter(Objects::nonNull)
                        .filter(event -> EventFilter.inCity(event, city))
                        .forEach(events::add);
                succeeded++;
            } catch (RuntimeException e) {
                // Never log the exception itself: messages of HTTP client errors may contain request URLs with keys.
                log.warn("Event provider {} failed for city {}: {}", provider.source().getCode(), city.getId(),
                        FailureDescriptions.describe(e));
                failure = e;
            }
        }
        if (succeeded == 0 && failure != null) {
            throw failure instanceof ServiceValidationException sve ? sve : upstreamUnavailable();
        }
        List<Event> inserted = eventIngestionService.upsert(events);
        log.info("Catalog refreshed for {}: {} events fetched, {} new", city.getId(), events.size(), inserted.size());
        if (!inserted.isEmpty()) {
            eventPublisher.publishEvent(new NewEventsIngestedEvent(city, inserted));
        }
        return inserted;
    }

    @Override
    public void ensureLoaded(SupportedCity city) {
        if (repository.exists(CatalogEventSpecifications.providerRowsInCity(city))) {
            return;
        }
        Instant now = clock.instant();
        Instant last = liveFetches.get(city.getId());
        if (last != null && now.isBefore(last.plus(ingestionProperties.getLiveFetchCooldown()))) {
            return;
        }
        liveFetches.put(city.getId(), now);
        try {
            refresh(city);
        } catch (ServiceValidationException e) {
            // Let the next request try again instead of waiting for the cooldown.
            liveFetches.remove(city.getId());
            if (!repository.exists(CatalogEventSpecifications.inCity(city, demoProperties.isEnabled()))) {
                throw e;
            }
            log.warn("Live fetch for {} failed; serving the stored catalog", city.getId());
        }
    }

    @Override
    public Optional<Event> importById(EventId id) {
        Optional<Event> found = eventProviders.stream()
                .filter(provider -> provider.source() == id.getSource())
                .map(provider -> provider.findById(id.getSourceId()))
                .flatMap(Optional::stream)
                .findFirst();
        found.ifPresent(event -> {
            List<Event> inserted = eventIngestionService.upsert(List.of(event));
            if (!inserted.isEmpty()) {
                supportedCityService.findByName(event.getCity(), event.getState())
                        .ifPresent(city -> eventPublisher.publishEvent(new NewEventsIngestedEvent(city, inserted)));
            }
        });
        return found;
    }

    private static ServiceValidationException upstreamUnavailable() {
        return new ServiceValidationException(ErrorCode.UPSTREAM_UNAVAILABLE, MessageCode.EVENTS_SOURCE_UNAVAILABLE);
    }
}
