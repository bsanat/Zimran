package kz.astyq.zimran.notification.service.impl;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.event.model.domain.StoredEvent;
import kz.astyq.zimran.notification.model.domain.DeliveryWindow;
import kz.astyq.zimran.notification.model.enums.NotificationKind;
import kz.astyq.zimran.notification.repository.NotificationRepository;
import kz.astyq.zimran.notification.service.NotificationCreationService;
import kz.astyq.zimran.notification.util.MessageCode;
import kz.astyq.zimran.notification.util.NotificationIds;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationCreationServiceImpl implements NotificationCreationService {

    static final NotificationKind KIND = NotificationKind.NEW_EVENT_MATCH;

    private final NotificationRepository repository;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Override
    @Transactional
    public Optional<String> createIfAbsent(RequestScope scope, StoredEvent event, DeliveryWindow window) {
        String publicId = NotificationIds.newPublicId();
        Instant now = clock.instant();
        String titleArgs = jsonMapper.writeValueAsString(List.of(event.getEvent().getTitle()));
        int created = repository.insertIfAbsent(publicId, scope.getScopeId(), event.getCatalogId(), KIND.name(),
                MessageCode.NOTIFICATION_NEW_EVENT_MATCH, titleArgs, event.getEvent().getCategorySlug().name(),
                window.getDeliverAfter(), window.getBatchKey(), now);
        return created > 0 ? Optional.of(publicId) : Optional.empty();
    }

    @Override
    public Set<Long> findNotifiedEventIds(RequestScope scope, Collection<Long> catalogIds) {
        if (catalogIds == null || catalogIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(repository.findNotifiedEventIds(scope.getScopeId(), KIND, catalogIds));
    }
}
