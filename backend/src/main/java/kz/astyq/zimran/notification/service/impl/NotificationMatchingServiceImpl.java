package kz.astyq.zimran.notification.service.impl;

import kz.astyq.zimran.alert.model.domain.AlertSettings;
import kz.astyq.zimran.alert.service.AlertPreferenceService;
import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.domain.StoredEvent;
import kz.astyq.zimran.event.service.CatalogEventLookupService;
import kz.astyq.zimran.event.util.EventFilter;
import kz.astyq.zimran.notification.model.domain.DeliveryWindow;
import kz.astyq.zimran.notification.model.domain.NotificationCheckResult;
import kz.astyq.zimran.notification.service.NotificationCreationService;
import kz.astyq.zimran.notification.service.NotificationMatchingService;
import kz.astyq.zimran.notification.util.AlertMatchRules;
import kz.astyq.zimran.notification.util.DeliveryWindows;
import kz.astyq.zimran.subscription.service.EntitlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Runs outside a transaction; each notification is created in its own, so one failure does not undo the others.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationMatchingServiceImpl implements NotificationMatchingService {

    private final CatalogEventLookupService catalogEventLookupService;
    private final AlertPreferenceService alertPreferenceService;
    private final EntitlementService entitlementService;
    private final SupportedCityService supportedCityService;
    private final NotificationCreationService notificationCreationService;
    private final Clock clock;

    @Override
    public int match(List<Event> newEvents) {
        if (newEvents == null || newEvents.isEmpty()) {
            return 0;
        }
        List<EventId> ids = newEvents.stream()
                .filter(Objects::nonNull)
                .filter(event -> !event.isDemo())
                .map(Event::getId)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return 0;
        }
        Map<String, SupportedCity> cities = new HashMap<>();
        Map<String, List<StoredEvent>> eventsByCity = new LinkedHashMap<>();
        for (StoredEvent stored : catalogEventLookupService.findStored(ids)) {
            supportedCityService.findByName(stored.getEvent().getCity(), stored.getEvent().getState())
                    .ifPresent(city -> {
                        cities.putIfAbsent(city.getId(), city);
                        eventsByCity.computeIfAbsent(city.getId(), id -> new ArrayList<>()).add(stored);
                    });
        }
        if (eventsByCity.isEmpty()) {
            return 0;
        }
        Instant now = clock.instant();
        int created = 0;
        for (AlertSettings settings : alertPreferenceService.findActiveInCities(eventsByCity.keySet())) {
            if (!entitlementService.isPremium(settings.getScope())) {
                continue;
            }
            SupportedCity city = cities.get(settings.getCityId());
            DeliveryWindow window = DeliveryWindows.of(settings.getFrequency(), now, city.getZone());
            created += createMatching(settings, eventsByCity.get(settings.getCityId()), window, now).size();
        }
        return created;
    }

    @Override
    public NotificationCheckResult matchDemoEvents(RequestScope scope) {
        Optional<AlertSettings> found = alertPreferenceService.find(scope).filter(AlertSettings::isActive);
        if (found.isEmpty()) {
            return NotificationCheckResult.EMPTY;
        }
        AlertSettings settings = found.get();
        Optional<SupportedCity> city = supportedCityService.findById(settings.getCityId());
        if (city.isEmpty()) {
            return NotificationCheckResult.EMPTY;
        }
        List<StoredEvent> inCity = catalogEventLookupService.findDemoFirstSeenSince(settings.getEnabledAt()).stream()
                .filter(stored -> EventFilter.inCity(stored.getEvent(), city.get()))
                .toList();
        Set<Long> notified = notificationCreationService.findNotifiedEventIds(scope,
                inCity.stream().map(StoredEvent::getCatalogId).toList());
        List<StoredEvent> unprocessed = inCity.stream()
                .filter(stored -> !notified.contains(stored.getCatalogId()))
                .toList();
        Instant now = clock.instant();
        List<String> createdIds = createMatching(settings, unprocessed, DeliveryWindows.demo(now), now);
        return NotificationCheckResult.builder()
                .checked(unprocessed.size())
                .created(createdIds.size())
                .notificationIds(createdIds)
                .build();
    }

    /**
     * Creates the scope's notifications for the matching events; a failure for one scope is logged and skipped.
     *
     * @return public ids of the created notifications
     */
    private List<String> createMatching(AlertSettings settings, List<StoredEvent> events, DeliveryWindow window,
                                        Instant now) {
        List<String> created = new ArrayList<>();
        for (StoredEvent stored : events) {
            if (!AlertMatchRules.matches(settings, stored, now)) {
                continue;
            }
            try {
                notificationCreationService.createIfAbsent(settings.getScope(), stored, window).ifPresent(created::add);
            } catch (RuntimeException e) {
                log.warn("Could not create a notification for {}: {}", settings.getScope().getScopeId(), e.toString());
            }
        }
        return created;
    }
}
