package kz.astyq.zimran.event.util;

import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Removes duplicate events: first the same {@link EventId}, then the same listing under different ids
 * (same title ignoring case and punctuation, same venue ignoring case, same start minute or day). Of each group the
 * first event with a known price is kept, else the first event; the result keeps first-occurrence order.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventDeduplicator {

    private static final Pattern PUNCTUATION = Pattern.compile("[\\p{P}\\p{S}]", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    public static List<Event> dedupe(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }
        List<Event> present = events.stream().filter(Objects::nonNull).toList();
        List<Event> byId = keepBest(present, Event::getId);
        return keepBest(byId, EventDeduplicator::contentKey);
    }

    /**
     * Key of the "same listing" rule: normalized title, venue and the start instant truncated to minutes.
     */
    public static String contentKey(Event event) {
        String venue = event.getVenue() != null ? normalize(event.getVenue()) : "";
        return normalizeTitle(event.getTitle()) + '|' + venue + '|' + when(event);
    }

    /**
     * Start minute when the time is known, else the day, else the raw date text (an undated event only
     * matches another undated one with the same text).
     */
    private static String when(Event event) {
        if (event.getStartAt() != null) {
            return event.getStartAt().toInstant().truncatedTo(ChronoUnit.MINUTES).toString();
        }
        if (event.getLocalDate() != null) {
            return event.getLocalDate().toString();
        }
        return "?" + (event.getDateText() != null ? normalize(event.getDateText()) : "");
    }

    static String normalizeTitle(String title) {
        return normalize(PUNCTUATION.matcher(title).replaceAll(" "));
    }

    private static String normalize(String text) {
        return WHITESPACE.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }

    private static <K> List<Event> keepBest(List<Event> events, Function<Event, K> keyOf) {
        Map<K, Event> kept = new LinkedHashMap<>();
        for (Event event : events) {
            kept.merge(keyOf.apply(event), event, EventDeduplicator::better);
        }
        return new ArrayList<>(kept.values());
    }

    private static Event better(Event kept, Event candidate) {
        return !kept.isPriceKnown() && candidate.isPriceKnown() ? candidate : kept;
    }
}
