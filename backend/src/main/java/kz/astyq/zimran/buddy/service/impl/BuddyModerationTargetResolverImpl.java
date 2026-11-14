package kz.astyq.zimran.buddy.service.impl;

import kz.astyq.zimran.buddy.model.entity.BuddyPost;
import kz.astyq.zimran.buddy.model.entity.BuddyRequest;
import kz.astyq.zimran.buddy.repository.BuddyPostRepository;
import kz.astyq.zimran.buddy.repository.BuddyRequestRepository;
import kz.astyq.zimran.moderation.model.enums.ModerationTargetKind;
import kz.astyq.zimran.moderation.service.ModerationTargetResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Authors of Buddy posts and requests, for reports and blocks.
 */
@Service
@RequiredArgsConstructor
public class BuddyModerationTargetResolverImpl implements ModerationTargetResolver {

    private final BuddyPostRepository buddyPostRepository;
    private final BuddyRequestRepository buddyRequestRepository;

    @Override
    public Optional<String> authorScopeId(ModerationTargetKind kind, String publicId) {
        return switch (kind) {
            case POST -> buddyPostRepository.findByPublicId(publicId).map(BuddyPost::getOwnerScopeId);
            case REQUEST -> buddyRequestRepository.findByPublicId(publicId).map(BuddyRequest::getSenderScopeId);
        };
    }
}
