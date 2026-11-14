package kz.astyq.zimran.event.service;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.enums.Interest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Read access to real provider events (currently Ticketmaster), cached. Used by the catalog and the planner.
 */
public interface EventQueryService {

    /**
     * Upcoming events in {@code city} from {@code startDate} to {@code endDate} (both inclusive, in the city's
     * timezone), deduplicated and sorted by start, then title. {@code interests} empty = every interest.
     * Throws {@code ServiceValidationException(UPSTREAM_UNAVAILABLE)} when the provider is down and no stale
     * copy is cached.
     */
    List<Event> findEvents(SupportedCity city, LocalDate startDate, LocalDate endDate, Set<Interest> interests);

    /**
     * One event, or empty if the provider does not know it.
     */
    Optional<Event> findById(EventId id);
}
