package kz.astyq.zimran.event.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.core.configuration.DemoProperties;
import kz.astyq.zimran.event.mapper.CatalogEventMapper;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventCriteria;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.entity.CatalogEvent;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.event.repository.CatalogEventRepository;
import kz.astyq.zimran.event.service.CatalogRefreshService;
import kz.astyq.zimran.event.service.EventQueryService;
import kz.astyq.zimran.event.util.CatalogEventSpecifications;
import kz.astyq.zimran.event.util.EventDeduplicator;
import kz.astyq.zimran.event.util.EventFilter;
import kz.astyq.zimran.event.util.EventOrdering;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Reads the persistent catalog: SQL narrows the rows ({@link CatalogEventSpecifications}), {@link EventFilter}
 * decides on the exact days, interests, keyword and major flag. A city whose catalog has no provider events yet
 * is fetched live first. Demo rows are visible only while {@code app.demo.enabled}.
 */
@Service
@RequiredArgsConstructor
public class EventQueryServiceImpl implements EventQueryService {

    private final CatalogEventRepository repository;
    private final CatalogEventMapper mapper;
    private final CatalogRefreshService catalogRefreshService;
    private final DemoProperties demoProperties;

    @Override
    public List<Event> search(EventCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria");
        SupportedCity city = criteria.getCity();
        catalogRefreshService.ensureLoaded(city);

        List<Event> matching = repository.findAll(CatalogEventSpecifications.matching(criteria, demoProperties.isEnabled()))
                .stream()
                .map(mapper::toDomain)
                .filter(event -> EventFilter.matches(event, criteria))
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

    /**
     * Any status, so links to cancelled or expired events keep working. An id the catalog does not know is
     * looked up at its provider and stored.
     */
    @Override
    public Optional<Event> findById(EventId id) {
        if (id == null) {
            return Optional.empty();
        }
        Optional<CatalogEvent> stored = repository.findByProviderAndProviderEventId(
                id.getSource().getCode(), id.getSourceId());
        if (stored.isPresent()) {
            return stored.filter(entity -> !entity.isDemo() || demoProperties.isEnabled()).map(mapper::toDomain);
        }
        return catalogRefreshService.importById(id);
    }

    @Override
    public Set<Interest> coveredInterests(SupportedCity city) {
        catalogRefreshService.ensureLoaded(city);
        Set<Interest> covered = EnumSet.noneOf(Interest.class);
        repository.findAll(CatalogEventSpecifications.activeInCity(city, demoProperties.isEnabled()))
                .forEach(entity -> covered.addAll(entity.getInterests()));
        return Collections.unmodifiableSet(covered);
    }
}
