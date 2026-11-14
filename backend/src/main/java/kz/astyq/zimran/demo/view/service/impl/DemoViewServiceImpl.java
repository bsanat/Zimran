package kz.astyq.zimran.demo.view.service.impl;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.model.enums.RateLimitAction;
import kz.astyq.zimran.core.service.RateLimitService;
import kz.astyq.zimran.demo.model.dto.DemoAlertCheckViewResponse;
import kz.astyq.zimran.demo.model.dto.DemoEventCreateRequest;
import kz.astyq.zimran.demo.service.DemoAccessService;
import kz.astyq.zimran.demo.service.DemoEventDraftService;
import kz.astyq.zimran.demo.view.service.DemoViewService;
import kz.astyq.zimran.event.mapper.EventMapper;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.dto.EventDetailsViewResponse;
import kz.astyq.zimran.event.service.DemoEventService;
import kz.astyq.zimran.notification.model.domain.NotificationCheckResult;
import kz.astyq.zimran.notification.service.NotificationMatchingService;
import kz.astyq.zimran.subscription.model.dto.SubscriptionViewResponse;
import kz.astyq.zimran.subscription.service.DemoEntitlementService;
import kz.astyq.zimran.subscription.service.SubscriptionSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DemoViewServiceImpl implements DemoViewService {

    private final DemoAccessService demoAccessService;
    private final DemoEntitlementService demoEntitlementService;
    private final SubscriptionSummaryService subscriptionSummaryService;
    private final RateLimitService rateLimitService;
    private final DemoEventDraftService demoEventDraftService;
    private final DemoEventService demoEventService;
    private final NotificationMatchingService notificationMatchingService;
    private final EventMapper eventMapper;

    @Override
    public SubscriptionViewResponse activatePremium(RequestScope scope) {
        demoAccessService.requireEnabled();
        demoEntitlementService.activateDemoPremium(scope);
        return subscriptionSummaryService.getSummary(scope);
    }

    @Override
    public EventDetailsViewResponse createEvent(DemoEventCreateRequest request, RequestScope scope) {
        demoAccessService.requireDemoPremium(scope);
        rateLimitService.consume(RateLimitAction.DEMO_EVENT, scope);
        Event event = demoEventService.addSample(demoEventDraftService.resolve(request));
        return EventDetailsViewResponse.builder().event(eventMapper.toViewResponse(event)).build();
    }

    @Override
    public DemoAlertCheckViewResponse checkAlerts(RequestScope scope) {
        demoAccessService.requireDemoPremium(scope);
        rateLimitService.consume(RateLimitAction.ALERTS_CHECK, scope);
        NotificationCheckResult result = notificationMatchingService.matchDemoEvents(scope);
        return DemoAlertCheckViewResponse.builder()
                .checked(result.getChecked())
                .created(result.getCreated())
                .notificationIds(result.getNotificationIds())
                .build();
    }
}
