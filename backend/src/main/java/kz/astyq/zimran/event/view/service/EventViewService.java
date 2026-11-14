package kz.astyq.zimran.event.view.service;

import kz.astyq.zimran.event.model.dto.EventDetailsViewResponse;
import kz.astyq.zimran.event.model.dto.EventPageViewResponse;
import kz.astyq.zimran.event.model.dto.EventSearchRequest;

/**
 * Public event catalog.
 */
public interface EventViewService {

    /**
     * One page of upcoming events. Invalid dates or an unsupported city: {@code VALIDATION_ERROR} with a
     * {@code field} detail; provider down: {@code UPSTREAM_UNAVAILABLE}.
     */
    EventPageViewResponse getPage(EventSearchRequest request);

    /**
     * One event by its public id ({@code ticketmaster:<id>}); unknown or malformed id: {@code NOT_FOUND}.
     */
    EventDetailsViewResponse getById(String id);
}
