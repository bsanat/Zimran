package kz.astyq.zimran.event.service;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventCriteria;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.enums.Interest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Read access to the real events of every {@link EventProvider}. Used by the catalog and the planner.
 */
public interface EventQueryService {

    /**
     * Events matching {@code criteria}, deduplicated and sorted by start (undated last), then title. A source
     * that is down: {@code ServiceValidationException(UPSTREAM_UNAVAILABLE)}.
     */
    List<Event> search(EventCriteria criteria);

    /**
     * One event, or empty if no source knows it.
     */
    Optional<Event> findById(EventId id);

    /**
     * Interests that have at least one event in {@code city}.
     */
    Set<Interest> coveredInterests(SupportedCity city);
}
