package kz.astyq.zimran.buddy.service.impl;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.core.util.PublicIds;
import kz.astyq.zimran.buddy.model.domain.BuddyRequestDraft;
import kz.astyq.zimran.buddy.model.domain.BuddyRequestWithPost;
import kz.astyq.zimran.buddy.model.entity.BuddyPost;
import kz.astyq.zimran.buddy.model.entity.BuddyRequest;
import kz.astyq.zimran.buddy.model.enums.BuddyDecision;
import kz.astyq.zimran.buddy.model.enums.BuddyPostStatus;
import kz.astyq.zimran.buddy.model.enums.BuddyRequestStatus;
import kz.astyq.zimran.buddy.repository.BuddyPostRepository;
import kz.astyq.zimran.buddy.repository.BuddyRequestRepository;
import kz.astyq.zimran.buddy.service.BuddyRequestService;
import kz.astyq.zimran.buddy.util.BuddyContentRules;
import kz.astyq.zimran.buddy.util.BuddyPostStatuses;
import kz.astyq.zimran.buddy.util.MessageCode;
import kz.astyq.zimran.moderation.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BuddyRequestServiceImpl implements BuddyRequestService {

    static final String PUBLIC_ID_PREFIX = "breq_";
    private static final Set<BuddyRequestStatus> ACTIVE = EnumSet.of(BuddyRequestStatus.PENDING,
            BuddyRequestStatus.ACCEPTED);

    private final BuddyPostRepository buddyPostRepository;
    private final BuddyRequestRepository buddyRequestRepository;
    private final BlockService blockService;
    private final Clock clock;

    @Override
    @Transactional
    public BuddyRequestWithPost create(RequestScope scope, String postId, BuddyRequestDraft draft) {
        BuddyContentRules.requireNoContactDetails(draft.getDisplayName(), "displayName");
        BuddyContentRules.requireNoContactDetails(draft.getMessage(), "message");
        BuddyPost found = buddyPostRepository.findByPublicId(postId).orElseThrow(() -> new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.POST_NOT_FOUND));
        BuddyPost post = lock(found);
        String sender = scope.getScopeId();
        if (post.getOwnerScopeId().equals(sender)) {
            throw new ServiceValidationException(ErrorCode.CONFLICT, MessageCode.OWN_POST);
        }
        if (!BuddyPostStatuses.isOpen(post, clock.instant())) {
            throw new ServiceValidationException(ErrorCode.CONFLICT, MessageCode.POST_NOT_OPEN);
        }
        if (blockService.isBlockedEitherWay(post.getOwnerScopeId(), sender)) {
            throw new ServiceValidationException(ErrorCode.FORBIDDEN, MessageCode.BLOCKED);
        }
        if (buddyRequestRepository.existsByPostIdAndSenderScopeId(post.getId(), sender)) {
            throw new ServiceValidationException(ErrorCode.CONFLICT, MessageCode.REQUEST_EXISTS);
        }
        try {
            BuddyRequest request = buddyRequestRepository.saveAndFlush(BuddyRequest.builder()
                    .publicId(PublicIds.newId(PUBLIC_ID_PREFIX))
                    .postId(post.getId())
                    .senderScopeId(sender)
                    .senderDisplayName(draft.getDisplayName())
                    .message(draft.getMessage())
                    .status(BuddyRequestStatus.PENDING)
                    .build());
            return BuddyRequestWithPost.builder().request(request).post(post).build();
        } catch (DataIntegrityViolationException e) {
            throw new ServiceValidationException(ErrorCode.CONFLICT, MessageCode.REQUEST_EXISTS).withCause(e);
        }
    }

    @Override
    @Transactional
    public BuddyRequestWithPost decide(RequestScope scope, String requestId, BuddyDecision decision) {
        BuddyRequest request = buddyRequestRepository.findByPublicId(requestId).orElseThrow(() -> new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.REQUEST_NOT_FOUND));
        BuddyPost post = buddyPostRepository.findByIdForUpdate(request.getPostId()).orElseThrow(() -> new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.REQUEST_NOT_FOUND));
        if (!post.getOwnerScopeId().equals(scope.getScopeId())) {
            throw new ServiceValidationException(ErrorCode.FORBIDDEN, MessageCode.NOT_POST_OWNER);
        }
        if (request.getStatus() != BuddyRequestStatus.PENDING) {
            throw new ServiceValidationException(ErrorCode.CONFLICT, MessageCode.REQUEST_NOT_PENDING);
        }
        Instant now = clock.instant();
        if (!BuddyPostStatuses.isOpen(post, now)) {
            throw new ServiceValidationException(ErrorCode.CONFLICT, MessageCode.POST_NOT_OPEN);
        }
        request.setResolvedAt(now);
        if (decision == BuddyDecision.DECLINE) {
            request.setStatus(BuddyRequestStatus.DECLINED);
            return BuddyRequestWithPost.builder().request(request).post(post).build();
        }
        request.setStatus(BuddyRequestStatus.ACCEPTED);
        post.setStatus(BuddyPostStatus.MATCHED);
        for (BuddyRequest other : buddyRequestRepository.findAllByPostIdAndStatus(post.getId(),
                BuddyRequestStatus.PENDING)) {
            if (!other.getId().equals(request.getId())) {
                other.setStatus(BuddyRequestStatus.DECLINED);
                other.setResolvedAt(now);
            }
        }
        return BuddyRequestWithPost.builder().request(request).post(post).build();
    }

    @Override
    @Transactional
    public BuddyRequestWithPost cancel(RequestScope scope, String requestId) {
        BuddyRequest request = buddyRequestRepository.findByPublicId(requestId).orElseThrow(() -> new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.REQUEST_NOT_FOUND));
        if (!request.getSenderScopeId().equals(scope.getScopeId())) {
            throw new ServiceValidationException(ErrorCode.FORBIDDEN, MessageCode.NOT_REQUEST_SENDER);
        }
        BuddyPost post = buddyPostRepository.findByIdForUpdate(request.getPostId()).orElseThrow(() -> new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.REQUEST_NOT_FOUND));
        if (!ACTIVE.contains(request.getStatus())) {
            throw new ServiceValidationException(ErrorCode.CONFLICT, MessageCode.REQUEST_NOT_ACTIVE);
        }
        cancelRequest(request, post, clock.instant());
        return BuddyRequestWithPost.builder().request(request).post(post).build();
    }

    @Override
    @Transactional
    public void cancelBetween(String firstScopeId, String secondScopeId) {
        Instant now = clock.instant();
        for (BuddyRequest request : buddyRequestRepository.findBetween(firstScopeId, secondScopeId, ACTIVE)) {
            buddyPostRepository.findByIdForUpdate(request.getPostId())
                    .ifPresent(post -> cancelRequest(request, post, now));
        }
    }

    private void cancelRequest(BuddyRequest request, BuddyPost post, Instant now) {
        boolean wasAccepted = request.getStatus() == BuddyRequestStatus.ACCEPTED;
        request.setStatus(BuddyRequestStatus.CANCELLED);
        request.setResolvedAt(now);
        if (wasAccepted && BuddyPostStatuses.effective(post, now) == BuddyPostStatus.MATCHED) {
            // Reopen for other requests, unless the owner already has a new OPEN post for this event.
            boolean otherOpen = buddyPostRepository.existsByOwnerScopeIdAndEventIdAndStatus(post.getOwnerScopeId(),
                    post.getEventId(), BuddyPostStatus.OPEN);
            post.setStatus(otherOpen ? BuddyPostStatus.CLOSED : BuddyPostStatus.OPEN);
        }
    }

    private BuddyPost lock(BuddyPost post) {
        return buddyPostRepository.findByIdForUpdate(post.getId()).orElseThrow(() -> new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.POST_NOT_FOUND));
    }
}
