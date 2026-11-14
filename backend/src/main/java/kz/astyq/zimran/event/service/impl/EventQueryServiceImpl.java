package kz.astyq.zimran.event.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.configuration.CacheConfig;
import kz.astyq.zimran.core.configuration.TicketmasterProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.domain.EventSearchKey;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.event.service.EventQueryService;
import kz.astyq.zimran.event.util.EventDeduplicator;
import kz.astyq.zimran.ticketmaster.client.TicketmasterClient;
import kz.astyq.zimran.ticketmaster.model.dto.TmEvent;
import kz.astyq.zimran.ticketmaster.model.dto.TmNamed;
import kz.astyq.zimran.ticketmaster.model.dto.TmSearchQuery;
import kz.astyq.zimran.ticketmaster.model.dto.TmVenue;
import kz.astyq.zimran.ticketmaster.service.InterestMappingService;
import kz.astyq.zimran.ticketmaster.service.TicketmasterEventNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Ticketmaster-backed {@link EventQueryService}. Each provider search is cached on its own (see
 * {@link CacheConfig}), so overlapping requests share results.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventQueryServiceImpl implements EventQueryService {

    static final int MAX_SEARCHES = 6;
    static final int SEARCH_PAGE_SIZE = 200;
    static final int MAX_EVENTS = 500;

    private static final Comparator<Event> BY_START_THEN_TITLE = Comparator
            .comparing((Event e) -> e.getStartAt().toInstant())
            .thenComparing(Event::getTitle, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Event::getTitle)
            .thenComparing(e -> e.getId().getSourceId());

    private final TicketmasterClient ticketmasterClient;
    private final TicketmasterEventNormalizer ticketmasterEventNormalizer;
    private final InterestMappingService interestMappingService;
    private final SupportedCityService supportedCityService;
    private final TicketmasterProperties ticketmasterProperties;
    private final CacheManager cacheManager;
    private final Clock clock;

    @Override
    public List<Event> findEvents(SupportedCity city, LocalDate startDate, LocalDate endDate,
                                  Set<Interest> interests) {
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        Set<Interest> wanted = interests == null || interests.isEmpty()
                ? EnumSet.noneOf(Interest.class) : EnumSet.copyOf(interests);

        Instant now = clock.instant();
        Instant dayStart = startDate.atStartOfDay(city.getZone()).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(city.getZone()).toInstant();
        if (!end.isAfter(now) || !end.isAfter(dayStart)) {
            return List.of();
        }
        Instant start = dayStart.isAfter(now) ? dayStart : searchStartFromNow(now);

        List<Event> found = new ArrayList<>();
        for (String classificationName : classificationNames(wanted)) {
            found.addAll(search(new EventSearchKey(city.getId(), start, end, classificationName), city));
        }

        List<Event> events = EventDeduplicator.dedupe(found.stream()
                .filter(event -> wanted.isEmpty() || !Collections.disjoint(event.getInterests(), wanted))
                .filter(event -> !event.getStartAt().toInstant().isBefore(now))
                .filter(event -> event.getStartAt().toInstant().isBefore(end))
                .toList());
        List<Event> result = events.stream().sorted(BY_START_THEN_TITLE).limit(MAX_EVENTS).toList();

        Cache byId = cache(CacheConfig.EVENT_BY_ID);
        result.forEach(event -> byId.put(event.getId(), event));
        return result;
    }

    @Override
    public Optional<Event> findById(EventId id) {
        if (id == null || id.getSource() != EventSource.TICKETMASTER) {
            return Optional.empty();
        }
        Cache byId = cache(CacheConfig.EVENT_BY_ID);
        Event cached = byId.get(id, Event.class);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<Event> event = ticketmasterClient.getEvent(id.getSourceId())
                .flatMap(tm -> ticketmasterEventNormalizer.normalize(tm, cityOf(tm)));
        event.ifPresent(e -> byId.put(e.getId(), e));
        return event;
    }

    /**
     * When the window starts "now", the provider query starts at the top of the current hour so the cache key
     * stays stable for repeated requests; events before {@code now} are filtered out afterwards.
     */
    private static Instant searchStartFromNow(Instant now) {
        return now.truncatedTo(ChronoUnit.HOURS);
    }

    /**
     * {@code [null]} (one search without classification) when no interest is requested; otherwise the distinct
     * classification names of the covered interests, at most {@link #MAX_SEARCHES}.
     */
    private List<String> classificationNames(Set<Interest> wanted) {
        if (wanted.isEmpty()) {
            return Collections.singletonList(null);
        }
        Set<String> names = new LinkedHashSet<>();
        for (Interest interest : wanted) {
            if (interestMappingService.hasCoverage(interest)) {
                names.addAll(interestMappingService.classificationNamesFor(interest));
            }
        }
        if (names.size() > MAX_SEARCHES) {
            log.debug("Interests {} need {} searches; using the first {}", wanted, names.size(), MAX_SEARCHES);
        }
        return names.stream().limit(MAX_SEARCHES).toList();
    }

    /**
     * Normalized events of one search, from {@code eventSearch} or Ticketmaster. If Ticketmaster is
     * unavailable, the copy in {@code eventSearchStale} is used when there is one.
     */
    @SuppressWarnings("unchecked")
    private List<Event> search(EventSearchKey key, SupportedCity city) {
        Cache fresh = cache(CacheConfig.EVENT_SEARCH);
        List<Event> cached = fresh.get(key, List.class);
        if (cached != null) {
            return cached;
        }
        Cache stale = cache(CacheConfig.EVENT_SEARCH_STALE);
        List<TmEvent> raw;
        try {
            raw = ticketmasterClient.searchAllPages(query(key, city), ticketmasterProperties.getMaxPagesPerSearch());
        } catch (ServiceValidationException e) {
            List<Event> staleCopy = e.getErrorCode() == ErrorCode.UPSTREAM_UNAVAILABLE
                    ? stale.get(key, List.class) : null;
            if (staleCopy == null) {
                throw e;
            }
            log.warn("Ticketmaster unavailable; serving stale results for city={} classification={} ({} events)",
                    key.getCityId(), key.getClassificationName(), staleCopy.size());
            return staleCopy;
        }
        List<Event> events = raw.stream()
                .map(tm -> ticketmasterEventNormalizer.normalize(tm, city))
                .flatMap(Optional::stream)
                .toList();
        fresh.put(key, events);
        stale.put(key, events);
        return events;
    }

    private static TmSearchQuery query(EventSearchKey key, SupportedCity city) {
        return TmSearchQuery.builder()
                .city(city.getName())
                .stateCode(city.getStateCode())
                .countryCode(city.getCountryCode())
                .start(key.getStart())
                .end(key.getEnd())
                .classificationName(key.getClassificationName())
                .page(0)
                .size(SEARCH_PAGE_SIZE)
                .build();
    }

    private SupportedCity cityOf(TmEvent tm) {
        String venueCity = tm.getEmbedded() == null || tm.getEmbedded().getVenues() == null ? null
                : tm.getEmbedded().getVenues().stream()
                        .filter(Objects::nonNull)
                        .findFirst()
                        .map(TmVenue::getCity)
                        .map(TmNamed::getName)
                        .orElse(null);
        return supportedCityService.findByName(venueCity).orElseGet(supportedCityService::getDefault);
    }

    private Cache cache(String name) {
        Cache cache = cacheManager.getCache(name);
        if (cache == null) {
            throw new IllegalStateException("Cache " + name + " is not configured");
        }
        return cache;
    }
}
