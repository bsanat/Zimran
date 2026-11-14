package kz.astyq.zimran.buddy.view.service.impl;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.i18n.MessageService;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.model.enums.RateLimitAction;
import kz.astyq.zimran.core.service.RateLimitService;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.buddy.mapper.BuddyMapper;
import kz.astyq.zimran.buddy.model.domain.BuddyOverview;
import kz.astyq.zimran.buddy.model.domain.BuddyRequestWithPost;
import kz.astyq.zimran.buddy.model.dto.BuddyDecisionRequest;
import kz.astyq.zimran.buddy.model.dto.BuddyIncomingRequestViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyMeViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyMyPostViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyOutgoingRequestViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyPostCreateRequest;
import kz.astyq.zimran.buddy.model.dto.BuddyPostListViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyPostViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyRequestCreateRequest;
import kz.astyq.zimran.buddy.model.entity.BuddyPost;
import kz.astyq.zimran.buddy.model.entity.BuddyRequest;
import kz.astyq.zimran.buddy.model.enums.BuddyRequestStatus;
import kz.astyq.zimran.buddy.service.BuddyPostService;
import kz.astyq.zimran.buddy.service.BuddyQueryService;
import kz.astyq.zimran.buddy.service.BuddyRequestService;
import kz.astyq.zimran.buddy.util.BuddyPostStatuses;
import kz.astyq.zimran.buddy.util.MessageCode;
import kz.astyq.zimran.subscription.service.EntitlementService;
import kz.astyq.zimran.buddy.view.service.BuddyViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BuddyViewServiceImpl implements BuddyViewService {

    private final RateLimitService rateLimitService;
    private final EntitlementService entitlementService;
    private final BuddyPostService buddyPostService;
    private final BuddyRequestService buddyRequestService;
    private final BuddyQueryService buddyQueryService;
    private final BuddyMapper buddyMapper;
    private final MessageService messageService;
    private final Clock clock;

    @Override
    public BuddyPostListViewResponse openPosts(String eventId, RequestScope viewer) {
        Instant now = clock.instant();
        String viewerId = viewer != null ? viewer.getScopeId() : null;
        return BuddyPostListViewResponse.builder()
                .posts(buddyQueryService.openPosts(eventId.strip(), viewer).stream()
                        .map(post -> post(post, viewerId, now))
                        .toList())
                .build();
    }

    @Override
    public BuddyMeViewResponse me(RequestScope scope) {
        Instant now = clock.instant();
        BuddyOverview overview = buddyQueryService.overview(scope);
        return BuddyMeViewResponse.builder()
                .posts(overview.getPosts().stream().map(mine -> BuddyMyPostViewResponse.builder()
                        .post(post(mine.getPost(), scope.getScopeId(), now))
                        .requests(mine.getRequests().stream().map(this::incoming).toList())
                        .build()).toList())
                .requests(overview.getRequests().stream().map(sent -> outgoing(sent, scope, now)).toList())
                .build();
    }

    @Override
    public BuddyPostViewResponse createPost(BuddyPostCreateRequest request, RequestScope scope) {
        rateLimitService.consume(RateLimitAction.BUDDY_POST, scope);
        requireMember(scope, request.getAgeConfirmed());
        return post(buddyPostService.create(scope, buddyMapper.toDraft(request)), scope.getScopeId(), clock.instant());
    }

    @Override
    public BuddyOutgoingRequestViewResponse createRequest(String postId, BuddyRequestCreateRequest request,
                                                          RequestScope scope) {
        rateLimitService.consume(RateLimitAction.BUDDY_REQUEST, scope);
        requireMember(scope, request.getAgeConfirmed());
        return outgoing(buddyRequestService.create(scope, postId, buddyMapper.toDraft(request)), scope, clock.instant());
    }

    @Override
    public BuddyIncomingRequestViewResponse decide(String requestId, BuddyDecisionRequest request, RequestScope scope) {
        return incoming(buddyRequestService.decide(scope, requestId, request.getDecision()).getRequest());
    }

    @Override
    public BuddyOutgoingRequestViewResponse cancel(String requestId, RequestScope scope) {
        return outgoing(buddyRequestService.cancel(scope, requestId), scope, clock.instant());
    }

    @Override
    public BuddyPostViewResponse close(String postId, RequestScope scope) {
        return post(buddyPostService.close(scope, postId), scope.getScopeId(), clock.instant());
    }

    /** Premium first (the paywall), then the age confirmation. */
    private void requireMember(RequestScope scope, Boolean ageConfirmed) {
        entitlementService.requirePremium(scope);
        if (!Boolean.TRUE.equals(ageConfirmed)) {
            throw new ServiceValidationException(ErrorCode.AGE_CONFIRMATION_REQUIRED, MessageCode.AGE_CONFIRMATION_REQUIRED)
                    .withDetail("field", "ageConfirmed");
        }
    }

    private BuddyPostViewResponse post(BuddyPost post, String viewerScopeId, Instant now) {
        return buddyMapper.toViewResponse(post, BuddyPostStatuses.effective(post, now),
                post.getOwnerScopeId().equals(viewerScopeId));
    }

    private BuddyIncomingRequestViewResponse incoming(BuddyRequest request) {
        return buddyMapper.toIncomingViewResponse(request, safetyNotice(request));
    }

    private BuddyOutgoingRequestViewResponse outgoing(BuddyRequestWithPost sent, RequestScope scope, Instant now) {
        return buddyMapper.toOutgoingViewResponse(sent.getRequest(), post(sent.getPost(), scope.getScopeId(), now),
                safetyNotice(sent.getRequest()));
    }

    /** Both sides of an accepted request see how to meet safely. */
    private String safetyNotice(BuddyRequest request) {
        return request.getStatus() == BuddyRequestStatus.ACCEPTED ? messageService.getMessage(MessageCode.SAFETY_NOTICE)
                : null;
    }
}
