package kz.astyq.zimran.event.service;

import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.domain.StoredEvent;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Reads catalog rows together with their internal bookkeeping, for features that reference catalog events
 * (e.g. notifications).
 */
public interface CatalogEventLookupService {

    /**
     * The stored rows of {@code ids}; ids not in the catalog are skipped.
     */
    List<StoredEvent> findStored(Collection<EventId> ids);

    /**
     * Events by internal catalog id; ids not in the catalog (or deleted) are missing from the map.
     */
    Map<Long, Event> findByCatalogIds(Collection<Long> catalogIds);

    /**
     * Demo sample events ({@code isDemo}) first seen at or after {@code since}, oldest first.
     */
    List<StoredEvent> findDemoFirstSeenSince(Instant since);
}
