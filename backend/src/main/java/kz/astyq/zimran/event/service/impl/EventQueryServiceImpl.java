package kz.astyq.zimran.event.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventCriteria;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.event.service.EventProvider;
import kz.astyq.zimran.event.service.EventQueryService;
import kz.astyq.zimran.event.util.EventDeduplicator;
import kz.astyq.zimran.event.util.EventOrdering;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Filters the events of every {@link EventProvider} in memory. Providers cache their own data.
 */
@Service
@RequiredArgsConstructor
public class EventQueryServiceImpl implements EventQueryService {

    private final List<EventProvider> eventProviders;

    @Override
    public List<Event> search(EventCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria");
        SupportedCity city = criteria.getCity();
        Set<Interest> interests = criteria.getInterests() == null || criteria.getInterests().isEmpty()
                ? EnumSet.noneOf(Interest.class) : EnumSet.copyOf(criteria.getInterests());
        String keyword = StringUtils.hasText(criteria.getKeyword())
                ? criteria.getKeyword().strip().toLowerCase(Locale.ROOT) : null;

        List<Event> matching = allEvents()
                .filter(event -> inCity(event, city))
                .filter(event -> inRange(event, criteria))
                .filter(event -> interests.isEmpty() || !Collections.disjoint(event.getInterests(), interests))
                .filter(event -> keyword == null || containsKeyword(event, keyword))
                .filter(event -> !criteria.isMajorOnly() || event.isMajor())
                .toList();
        // A multi-day event sorts by its first day in the searched range, so a running exhibition is listed
        // with today's events instead of on the day it opened.
        Map<Event, LocalDate> firstDays = new IdentityHashMap<>();
        matching.forEach(event -> firstDays.put(event,
                event.firstDayBetween(criteria.getFromDate(), criteria.getToDate(), city.getZone())));
        Comparator<Event> order = Comparator
                .comparing((Event event) -> firstDays.get(event), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(EventOrdering.BY_START_THEN_TITLE);
        return EventDeduplicator.dedupe(matching).stream().sorted(order).toList();
    }

    @Override
    public Optional<Event> findById(EventId id) {
        if (id == null) {
            return Optional.empty();
        }
        return eventProviders.stream()
                .filter(provider -> provider.source() == id.getSource())
                .map(provider -> provider.findById(id.getSourceId()))
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Override
    public Set<Interest> coveredInterests(SupportedCity city) {
        Set<Interest> covered = EnumSet.noneOf(Interest.class);
        allEvents().filter(event -> inCity(event, city)).forEach(event -> covered.addAll(event.getInterests()));
        return Collections.unmodifiableSet(covered);
    }

    private Stream<Event> allEvents() {
        return eventProviders.stream().flatMap(provider -> provider.getAll().stream()).filter(Objects::nonNull);
    }

    /**
     * Same city name; when both the event and the city have a state, the same state too (two cities may share a
     * name, e.g. Portland OR and Portland ME).
     */
    private static boolean inCity(Event event, SupportedCity city) {
        if (!event.getCity().strip().equalsIgnoreCase(city.getName())) {
            return false;
        }
        return !StringUtils.hasText(event.getState()) || !StringUtils.hasText(city.getStateCode())
                || event.getState().strip().equalsIgnoreCase(city.getStateCode().strip());
    }

    /**
     * Whether the event happens on at least one day of the range (a multi-day event matches when any of its
     * days does). Undated events cannot be checked and are kept only when the criteria ask for them.
     */
    private static boolean inRange(Event event, EventCriteria criteria) {
        if (!event.isDated()) {
            return criteria.isIncludeUndated();
        }
        if (criteria.getFromDate() == null && criteria.getToDate() == null) {
            return true;
        }
        return event.firstDayBetween(criteria.getFromDate(), criteria.getToDate(), criteria.getCity().getZone()) != null;
    }

    private static boolean containsKeyword(Event event, String keyword) {
        return contains(event.getTitle(), keyword)
                || contains(event.getDescription(), keyword)
                || contains(event.getVenue(), keyword);
    }

    private static boolean contains(String text, String keyword) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
