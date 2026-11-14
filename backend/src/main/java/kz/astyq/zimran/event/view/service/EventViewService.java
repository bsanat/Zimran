package kz.astyq.zimran.event.view.service;

import kz.astyq.zimran.event.model.dto.EventDetailsViewResponse;
import kz.astyq.zimran.event.model.dto.EventPageViewResponse;
import kz.astyq.zimran.event.model.dto.EventSearchRequest;

/**
 * Public event catalog.
 */
public interface EventViewService {

    /**
     * One page (10 events) of upcoming events. Unsupported city/state or a past date: {@code VALIDATION_ERROR}
     * with a {@code field} detail; source down: {@code UPSTREAM_UNAVAILABLE}.
     */
    EventPageViewResponse getPage(EventSearchRequest request);

    /**
     * One event by its public id ({@code <source>:<sourceEventId>}); unknown or malformed id: {@code NOT_FOUND}.
     */
    EventDetailsViewResponse getById(String id);
}
