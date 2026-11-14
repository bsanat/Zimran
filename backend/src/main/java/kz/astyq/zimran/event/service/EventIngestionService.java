package kz.astyq.zimran.event.service;

import kz.astyq.zimran.event.model.domain.Event;

import java.util.Collection;
import java.util.List;

/**
 * Writes events into the persistent catalog.
 */
public interface EventIngestionService {

    /**
     * Inserts or updates each event by provider + provider event id in one transaction. {@code firstSeenAt} is set
     * only on insert, {@code fetchedAt} on every write. Events whose every day has passed end up {@code EXPIRED}.
     * Duplicate ids in {@code events}: the first one wins.
     *
     * @return the events that were not in the catalog before, in input order
     */
    List<Event> upsert(Collection<Event> events);

    /**
     * Marks every catalog event whose last day has passed as {@code EXPIRED}.
     *
     * @return the number of events expired
     */
    int expirePastEvents();
}
