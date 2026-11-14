package kz.astyq.zimran.event.service.impl;

import kz.astyq.zimran.event.mapper.CatalogEventMapper;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.domain.StoredEvent;
import kz.astyq.zimran.event.model.entity.CatalogEvent;
import kz.astyq.zimran.event.repository.CatalogEventRepository;
import kz.astyq.zimran.event.service.CatalogEventLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogEventLookupServiceImpl implements CatalogEventLookupService {

    static final int LOOKUP_CHUNK = 500;

    private final CatalogEventRepository repository;
    private final CatalogEventMapper mapper;

    @Override
    public List<StoredEvent> findStored(Collection<EventId> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<String, List<String>> idsByProvider = ids.stream().filter(Objects::nonNull).distinct()
                .collect(Collectors.groupingBy(id -> id.getSource().getCode(), LinkedHashMap::new,
                        Collectors.mapping(EventId::getSourceId, Collectors.toList())));
        List<StoredEvent> stored = new ArrayList<>();
        idsByProvider.forEach((provider, providerIds) -> {
            for (int from = 0; from < providerIds.size(); from += LOOKUP_CHUNK) {
                List<String> chunk = providerIds.subList(from, Math.min(from + LOOKUP_CHUNK, providerIds.size()));
                repository.findByProviderAndProviderEventIdIn(provider, chunk).forEach(entity -> stored.add(toStored(entity)));
            }
        });
        return stored;
    }

    @Override
    public Map<Long, Event> findByCatalogIds(Collection<Long> catalogIds) {
        if (catalogIds == null || catalogIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Event> events = new HashMap<>();
        repository.findAllById(catalogIds.stream().filter(Objects::nonNull).distinct().toList())
                .forEach(entity -> events.put(entity.getId(), mapper.toDomain(entity)));
        return events;
    }

    @Override
    public List<StoredEvent> findDemoFirstSeenSince(Instant since) {
        Objects.requireNonNull(since, "since");
        return repository.findByDemoTrueAndFirstSeenAtGreaterThanEqualOrderByFirstSeenAtAscIdAsc(since).stream()
                .map(this::toStored)
                .toList();
    }

    private StoredEvent toStored(CatalogEvent entity) {
        return StoredEvent.builder()
                .catalogId(entity.getId())
                .event(mapper.toDomain(entity))
                .firstSeenAt(entity.getFirstSeenAt())
                .build();
    }
}
