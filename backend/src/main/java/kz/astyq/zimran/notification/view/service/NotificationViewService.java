package kz.astyq.zimran.notification.view.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.notification.model.dto.NotificationPageViewResponse;
import kz.astyq.zimran.notification.model.dto.NotificationSearchRequest;

public interface NotificationViewService {

    NotificationPageViewResponse getPage(NotificationSearchRequest request, RequestScope scope);

    void markRead(String notificationId, RequestScope scope);

    void markAllRead(RequestScope scope);
}
