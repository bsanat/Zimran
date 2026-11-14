package kz.astyq.zimran.event.service;

import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.enums.EventSource;

import java.util.List;
import java.util.Optional;

/**
 * One source of normalized events (e.g. the JSON feed). Implementations own loading, normalization and caching;
 * the catalog only filters what they return. Add a source by adding an implementation, nothing else changes.
 */
public interface EventProvider {

    /**
     * The source whose ids this provider resolves.
     */
    EventSource source();

    /**
     * Every event the source currently has. Source unreadable and nothing loaded before:
     * {@code ServiceValidationException(UPSTREAM_UNAVAILABLE)}.
     */
    List<Event> getAll();

    /**
     * One event by the source's own id, or empty when the source does not have it.
     */
    Optional<Event> findById(String sourceId);
}
