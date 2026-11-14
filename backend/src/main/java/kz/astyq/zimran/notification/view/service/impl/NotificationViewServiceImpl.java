package kz.astyq.zimran.notification.view.service.impl;

import kz.astyq.zimran.core.i18n.MessageService;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.dto.PaginationViewResponse;
import kz.astyq.zimran.event.service.CatalogEventLookupService;
import kz.astyq.zimran.notification.mapper.NotificationMapper;
import kz.astyq.zimran.notification.model.domain.NotificationInbox;
import kz.astyq.zimran.notification.model.dto.NotificationPageViewResponse;
import kz.astyq.zimran.notification.model.dto.NotificationSearchRequest;
import kz.astyq.zimran.notification.model.dto.NotificationViewResponse;
import kz.astyq.zimran.notification.model.entity.Notification;
import kz.astyq.zimran.notification.service.NotificationInboxService;
import kz.astyq.zimran.notification.view.service.NotificationViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationViewServiceImpl implements NotificationViewService {

    private final NotificationInboxService notificationInboxService;
    private final CatalogEventLookupService catalogEventLookupService;
    private final MessageService messageService;
    private final NotificationMapper mapper;

    @Override
    public NotificationPageViewResponse getPage(NotificationSearchRequest request, RequestScope scope) {
        int page = request.pageOrDefault();
        int size = request.sizeOrDefault();
        NotificationInbox inbox = notificationInboxService.getPage(scope, page, size);
        Map<Long, Event> events = catalogEventLookupService.findByCatalogIds(
                inbox.getItems().stream().map(Notification::getEventId).toList());
        List<NotificationViewResponse> items = inbox.getItems().stream()
                .map(notification -> mapper.toViewResponse(notification, title(notification),
                        events.get(notification.getEventId())))
                .toList();
        return NotificationPageViewResponse.builder()
                .unreadCount(inbox.getUnreadCount())
                .items(items)
                .pagination(PaginationViewResponse.builder()
                        .page(page)
                        .pageSize(size)
                        .hasMore(inbox.isHasMore())
                        .build())
                .build();
    }

    @Override
    public void markRead(String notificationId, RequestScope scope) {
        notificationInboxService.markRead(scope, notificationId);
    }

    @Override
    public void markAllRead(RequestScope scope) {
        notificationInboxService.markAllRead(scope);
    }

    private String title(Notification notification) {
        List<String> args = notification.getTitleArgs() != null ? notification.getTitleArgs() : List.of();
        return messageService.getMessage(notification.getTitleKey(), args.toArray());
    }
}
