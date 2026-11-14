package kz.astyq.zimran.buddy.model.domain;

import kz.astyq.zimran.event.model.domain.EventId;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;

/**
 * The upcoming occurrence of an event a Buddy post is for.
 */
@Value
@Builder
public class BuddyEventSlot {

    @NonNull
    EventId eventId;
    /** Start of the next occurrence; the end of that day when the event has no time. */
    @NonNull
    Instant expiresAt;
}
