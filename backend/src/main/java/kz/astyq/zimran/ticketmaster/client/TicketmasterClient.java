package kz.astyq.zimran.ticketmaster.client;

import kz.astyq.zimran.ticketmaster.model.dto.TmEvent;
import kz.astyq.zimran.ticketmaster.model.dto.TmSearchQuery;
import kz.astyq.zimran.ticketmaster.model.dto.TmSearchResponse;

import java.util.List;
import java.util.Optional;

/**
 * Ticketmaster Discovery API v2. Every failure surfaces as a {@code ServiceValidationException} with
 * {@code UPSTREAM_UNAVAILABLE}.
 */
public interface TicketmasterClient {

    /**
     * One page of search results. An empty result has no {@code _embedded}; use
     * {@link TmSearchResponse#events()} to read events null-safely.
     */
    TmSearchResponse searchEvents(TmSearchQuery query);

    /**
     * One event by its Ticketmaster id, or empty if Ticketmaster answers 404.
     */
    Optional<TmEvent> getEvent(String sourceEventId);

    /**
     * Consecutive pages starting at {@code query.page}. Stops after the last page, after
     * {@code maxPages} pages, or when the next page would cross the deep-paging cap
     * ({@code (page + 1) * size >= 1000}).
     */
    List<TmEvent> searchAllPages(TmSearchQuery query, int maxPages);
}
