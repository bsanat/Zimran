package kz.astyq.zimran.jsonsource.service.impl;

import kz.astyq.zimran.core.configuration.EventSourceProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.service.EventProvider;
import kz.astyq.zimran.jsonsource.client.EventJsonClient;
import kz.astyq.zimran.jsonsource.service.JsonEventNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link EventProvider} for the JSON feed. The parsed events are kept in memory; after
 * {@code app.events-source.refresh-interval} the next request checks the source again: a file is re-read only
 * when its size or modification time changed, a URL is downloaded again. When a reload fails the previous copy
 * keeps being served; with no copy yet, the error ({@code UPSTREAM_UNAVAILABLE}) reaches the caller.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JsonEventProviderImpl implements EventProvider {

    private final EventJsonClient eventJsonClient;
    private final JsonEventNormalizer jsonEventNormalizer;
    private final EventSourceProperties properties;
    private final Clock clock;

    private final Object lock = new Object();
    private volatile Snapshot snapshot;

    @Override
    public EventSource source() {
        return EventSource.JSON;
    }

    @Override
    public List<Event> getAll() {
        return current().events;
    }

    @Override
    public Optional<Event> findById(String sourceId) {
        return Optional.ofNullable(current().byId.get(sourceId));
    }

    private Snapshot current() {
        Snapshot loaded = snapshot;
        if (loaded != null && isFresh(loaded)) {
            return loaded;
        }
        synchronized (lock) {
            loaded = snapshot;
            if (loaded != null && isFresh(loaded)) {
                return loaded;
            }
            snapshot = refresh(loaded);
            return snapshot;
        }
    }

    private boolean isFresh(Snapshot loaded) {
        return clock.instant().isBefore(loaded.checkedAt.plus(properties.getRefreshInterval()));
    }

    private Snapshot refresh(Snapshot previous) {
        Instant now = clock.instant();
        String fingerprint = eventJsonClient.fingerprint();
        if (previous != null && fingerprint != null && fingerprint.equals(previous.fingerprint)) {
            return previous.checkedAgainAt(now);
        }
        try {
            List<Event> events = jsonEventNormalizer.normalizeAll(eventJsonClient.fetch());
            log.info("Event feed loaded: {} events", events.size());
            return new Snapshot(events, fingerprint, now);
        } catch (ServiceValidationException e) {
            if (previous == null) {
                throw e;
            }
            log.warn("Event feed reload failed; serving the previous {} events", previous.events.size(), e.getCause());
            return previous.checkedAgainAt(now);
        }
    }

    private static final class Snapshot {
        private final List<Event> events;
        private final Map<String, Event> byId;
        private final String fingerprint;
        private final Instant checkedAt;

        private Snapshot(List<Event> events, String fingerprint, Instant checkedAt) {
            Map<String, Event> index = new LinkedHashMap<>();
            events.stream().filter(Objects::nonNull).forEach(event -> index.putIfAbsent(event.getId().getSourceId(), event));
            this.events = List.copyOf(index.values());
            this.byId = Map.copyOf(index);
            this.fingerprint = fingerprint;
            this.checkedAt = checkedAt;
        }

        private Snapshot(Snapshot other, Instant checkedAt) {
            this.events = other.events;
            this.byId = other.byId;
            this.fingerprint = other.fingerprint;
            this.checkedAt = checkedAt;
        }

        private Snapshot checkedAgainAt(Instant checkedAt) {
            return new Snapshot(this, checkedAt);
        }
    }
}
