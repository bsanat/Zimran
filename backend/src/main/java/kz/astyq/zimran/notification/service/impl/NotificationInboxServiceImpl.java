package kz.astyq.zimran.notification.service.impl;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.notification.model.domain.NotificationInbox;
import kz.astyq.zimran.notification.model.entity.Notification;
import kz.astyq.zimran.notification.repository.NotificationRepository;
import kz.astyq.zimran.notification.service.NotificationInboxService;
import kz.astyq.zimran.notification.util.MessageCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationInboxServiceImpl implements NotificationInboxService {

    static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("deliverAfter"), Sort.Order.desc("id"));

    private final NotificationRepository repository;
    private final Clock clock;

    @Override
    public NotificationInbox getPage(RequestScope scope, int page, int size) {
        Instant now = clock.instant();
        Slice<Notification> slice = repository.findByScopeIdAndDeliverAfterLessThanEqual(scope.getScopeId(), now,
                PageRequest.of(page - 1, size, NEWEST_FIRST));
        return NotificationInbox.builder()
                .items(slice.getContent())
                .unreadCount(repository.countByScopeIdAndDeliverAfterLessThanEqualAndReadAtIsNull(scope.getScopeId(), now))
                .hasMore(slice.hasNext())
                .build();
    }

    @Override
    @Transactional
    public void markRead(RequestScope scope, String notificationId) {
        Instant now = clock.instant();
        Notification notification = repository
                .findByPublicIdAndScopeIdAndDeliverAfterLessThanEqual(notificationId, scope.getScopeId(), now)
                .orElseThrow(() -> new ServiceValidationException(ErrorCode.NOT_FOUND,
                        MessageCode.NOTIFICATION_NOT_FOUND));
        if (notification.getReadAt() == null) {
            notification.setReadAt(now);
        }
    }

    @Override
    @Transactional
    public int markAllRead(RequestScope scope) {
        return repository.markAllRead(scope.getScopeId(), clock.instant());
    }
}
