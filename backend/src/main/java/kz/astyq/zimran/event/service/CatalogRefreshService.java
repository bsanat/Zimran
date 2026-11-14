package kz.astyq.zimran.event.service;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;

import java.util.List;
import java.util.Optional;

/**
 * Pulls events from the {@link EventProvider}s into the catalog and announces the new ones with a
 * {@code NewEventsIngestedEvent}.
 */
public interface CatalogRefreshService {

    /**
     * Fetches {@code city}'s events from every provider and upserts them. A provider that fails is logged (without
     * secrets) and skipped; when every provider fails: {@code ServiceValidationException(UPSTREAM_UNAVAILABLE)}.
     *
     * @return the newly inserted events
     */
    List<Event> refresh(SupportedCity city);

    /**
     * Live fetch for a city the catalog has no provider events for yet, at most once per
     * {@code app.ingestion.live-fetch-cooldown}. A failed fetch is rethrown only while the city has no rows at all.
     */
    void ensureLoaded(SupportedCity city);

    /**
     * Looks up an event the catalog does not have at its provider and stores it; empty when no provider knows it.
     */
    Optional<Event> importById(EventId id);
}
