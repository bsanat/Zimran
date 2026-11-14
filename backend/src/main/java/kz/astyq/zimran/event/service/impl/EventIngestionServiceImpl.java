package kz.astyq.zimran.event.service.impl;

import kz.astyq.zimran.event.mapper.CatalogEventMapper;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.entity.CatalogEvent;
import kz.astyq.zimran.event.repository.CatalogEventRepository;
import kz.astyq.zimran.event.service.EventIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Upserts under a Postgres advisory lock, so concurrent runs (the job and a live fetch, or two instances) never
 * insert the same event twice and agree on which run inserted it.
 */
@Service
@RequiredArgsConstructor
public class EventIngestionServiceImpl implements EventIngestionService {

    /** Advisory lock key of catalog writes ("events" in ASCII). */
    static final long LOCK_KEY = 0x6576656e7473L;
    static final int LOOKUP_CHUNK = 500;

    private final CatalogEventRepository repository;
    private final CatalogEventMapper mapper;
    private final Clock clock;

    @Override
    @Transactional
    public List<Event> upsert(Collection<Event> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }
        Map<EventId, Event> byId = new LinkedHashMap<>();
        events.stream().filter(Objects::nonNull).forEach(event -> byId.putIfAbsent(event.getId(), event));
        if (byId.isEmpty()) {
            return List.of();
        }
        repository.lockForUpdate(LOCK_KEY);
        Instant now = clock.instant();
        Map<EventId, CatalogEvent> existing = findExisting(byId.values());

        List<CatalogEvent> toInsert = new ArrayList<>();
        List<Event> inserted = new ArrayList<>();
        for (Event event : byId.values()) {
            CatalogEvent entity = existing.get(event.getId());
            if (entity != null) {
                mapper.updateEntity(event, entity);
            } else {
                entity = mapper.toEntity(event);
                entity.setFirstSeenAt(now);
                toInsert.add(entity);
                inserted.add(event);
            }
            entity.setFetchedAt(now);
        }
        repository.saveAll(toInsert);
        repository.expireEndedBefore(now);
        return List.copyOf(inserted);
    }

    @Override
    @Transactional
    public int expirePastEvents() {
        return repository.expireEndedBefore(clock.instant());
    }

    private Map<EventId, CatalogEvent> findExisting(Collection<Event> events) {
        Map<String, List<String>> idsByProvider = events.stream().collect(Collectors.groupingBy(
                event -> event.getId().getSource().getCode(), LinkedHashMap::new,
                Collectors.mapping(event -> event.getId().getSourceId(), Collectors.toList())));
        Map<EventId, CatalogEvent> existing = new HashMap<>();
        idsByProvider.forEach((provider, ids) -> {
            for (int from = 0; from < ids.size(); from += LOOKUP_CHUNK) {
                List<String> chunk = ids.subList(from, Math.min(from + LOOKUP_CHUNK, ids.size()));
                repository.findByProviderAndProviderEventIdIn(provider, chunk)
                        .forEach(entity -> existing.put(mapper.toEventId(entity), entity));
            }
        });
        return existing;
    }
}
