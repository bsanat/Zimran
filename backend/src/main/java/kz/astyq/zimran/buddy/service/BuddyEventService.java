package kz.astyq.zimran.buddy.service;

import kz.astyq.zimran.buddy.model.domain.BuddyEventSlot;

/**
 * The events Buddy posts can be made for.
 */
public interface BuddyEventService {

    /**
     * The event's next occurrence that has not started yet. Unknown event: {@code 404 NOT_FOUND}; cancelled,
     * postponed, past or undated: {@code 400 VALIDATION_ERROR} ({@code field = eventId}).
     */
    BuddyEventSlot requireUpcoming(String eventId);
}
