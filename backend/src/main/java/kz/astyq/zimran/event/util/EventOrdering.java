package kz.astyq.zimran.event.util;

import kz.astyq.zimran.event.model.domain.Event;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Comparator;

/**
 * Orderings of events shared by the catalog and the planner.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventOrdering {

    /**
     * By start instant (a date without time counts as the start of that day in the event's timezone; events
     * without a date go last), then title ignoring case, then id for a stable order.
     */
    public static final Comparator<Event> BY_START_THEN_TITLE = Comparator
            .comparing(EventOrdering::sortInstant, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Event::getTitle, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Event::getTitle)
            .thenComparing(event -> event.getId().asString());

    /**
     * The instant an event is sorted by, or null when its date is unknown.
     */
    public static Instant sortInstant(Event event) {
        if (event.getStartAt() != null) {
            return event.getStartAt().toInstant();
        }
        if (event.getLocalDate() != null) {
            return event.getLocalDate().atStartOfDay(event.getTimezone()).toInstant();
        }
        return null;
    }
}
