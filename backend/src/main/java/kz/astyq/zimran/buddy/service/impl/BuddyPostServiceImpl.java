package kz.astyq.zimran.buddy.service.impl;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.core.util.PublicIds;
import kz.astyq.zimran.buddy.model.domain.BuddyEventSlot;
import kz.astyq.zimran.buddy.model.domain.BuddyPostDraft;
import kz.astyq.zimran.buddy.model.entity.BuddyPost;
import kz.astyq.zimran.buddy.model.entity.BuddyRequest;
import kz.astyq.zimran.buddy.model.enums.BuddyPostStatus;
import kz.astyq.zimran.buddy.model.enums.BuddyRequestStatus;
import kz.astyq.zimran.buddy.repository.BuddyPostRepository;
import kz.astyq.zimran.buddy.repository.BuddyRequestRepository;
import kz.astyq.zimran.buddy.service.BuddyEventService;
import kz.astyq.zimran.buddy.service.BuddyPostService;
import kz.astyq.zimran.buddy.util.BuddyContentRules;
import kz.astyq.zimran.buddy.util.BuddyPostStatuses;
import kz.astyq.zimran.buddy.util.MessageCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BuddyPostServiceImpl implements BuddyPostService {

    static final String PUBLIC_ID_PREFIX = "bud_";

    private final BuddyPostRepository buddyPostRepository;
    private final BuddyRequestRepository buddyRequestRepository;
    private final BuddyEventService buddyEventService;
    private final Clock clock;

    @Override
    @Transactional
    public BuddyPost create(RequestScope scope, BuddyPostDraft draft) {
        BuddyContentRules.requireNoContactDetails(draft.getDisplayName(), "displayName");
        BuddyContentRules.requireNoContactDetails(draft.getShortBio(), "shortBio");
        BuddyContentRules.requireNoContactDetails(draft.getPreferences(), "preferences");
        if (draft.getAgeMin() != null && draft.getAgeMax() != null && draft.getAgeMin() > draft.getAgeMax()) {
            throw new ServiceValidationException(ErrorCode.VALIDATION_ERROR, MessageCode.AGE_RANGE_INVALID)
                    .withDetail("field", "ageMax");
        }
        BuddyEventSlot slot = buddyEventService.requireUpcoming(draft.getEventId());
        String eventId = slot.getEventId().asString();
        if (buddyPostRepository.existsByOwnerScopeIdAndEventIdAndStatus(scope.getScopeId(), eventId,
                BuddyPostStatus.OPEN)) {
            throw postExists();
        }
        try {
            return buddyPostRepository.saveAndFlush(BuddyPost.builder()
                    .publicId(PublicIds.newId(PUBLIC_ID_PREFIX))
                    .eventId(eventId)
                    .ownerScopeId(scope.getScopeId())
                    .displayName(draft.getDisplayName())
                    .shortBio(draft.getShortBio())
                    .language(draft.getLanguage())
                    .preferences(draft.getPreferences())
                    .ageMin(draft.getAgeMin())
                    .ageMax(draft.getAgeMax())
                    .status(BuddyPostStatus.OPEN)
                    .expiresAt(slot.getExpiresAt())
                    .build());
        } catch (DataIntegrityViolationException e) {
            // A concurrent request created the scope's OPEN post for this event first.
            throw postExists().withCause(e);
        }
    }

    @Override
    @Transactional
    public BuddyPost close(RequestScope scope, String postId) {
        BuddyPost found = buddyPostRepository.findByPublicId(postId).orElseThrow(() -> new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.POST_NOT_FOUND));
        if (!found.getOwnerScopeId().equals(scope.getScopeId())) {
            throw new ServiceValidationException(ErrorCode.FORBIDDEN, MessageCode.NOT_POST_OWNER);
        }
        BuddyPost post = buddyPostRepository.findByIdForUpdate(found.getId()).orElseThrow(() -> new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.POST_NOT_FOUND));
        Instant now = clock.instant();
        if (!BuddyPostStatuses.ACTIVE.contains(BuddyPostStatuses.effective(post, now))) {
            throw new ServiceValidationException(ErrorCode.CONFLICT, MessageCode.POST_NOT_OPEN);
        }
        post.setStatus(BuddyPostStatus.CLOSED);
        for (BuddyRequest request : buddyRequestRepository.findAllByPostIdAndStatus(post.getId(),
                BuddyRequestStatus.PENDING)) {
            request.setStatus(BuddyRequestStatus.DECLINED);
            request.setResolvedAt(now);
        }
        return post;
    }

    private static ServiceValidationException postExists() {
        return new ServiceValidationException(ErrorCode.CONFLICT, MessageCode.POST_EXISTS).withDetail("field", "eventId");
    }
}
