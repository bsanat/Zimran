package kz.astyq.zimran.buddy.view.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.buddy.model.dto.BuddyDecisionRequest;
import kz.astyq.zimran.buddy.model.dto.BuddyIncomingRequestViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyMeViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyOutgoingRequestViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyPostCreateRequest;
import kz.astyq.zimran.buddy.model.dto.BuddyPostListViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyPostViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyRequestCreateRequest;

/**
 * Event Buddy. Posting and requesting need Premium (demo Premium counts) and a confirmed age of 18+; reading, closing,
 * deciding and cancelling do not.
 */
public interface BuddyViewService {

    /** @param viewer the caller's scope, or null */
    BuddyPostListViewResponse openPosts(String eventId, RequestScope viewer);

    BuddyMeViewResponse me(RequestScope scope);

    /**
     * Rate limited; Premium ({@code 403 PREMIUM_REQUIRED}); {@code ageConfirmed} must be true
     * ({@code 422 AGE_CONFIRMATION_REQUIRED}).
     */
    BuddyPostViewResponse createPost(BuddyPostCreateRequest request, RequestScope scope);

    /** Rate limited; Premium and {@code ageConfirmed} like {@link #createPost}. */
    BuddyOutgoingRequestViewResponse createRequest(String postId, BuddyRequestCreateRequest request, RequestScope scope);

    BuddyIncomingRequestViewResponse decide(String requestId, BuddyDecisionRequest request, RequestScope scope);

    BuddyOutgoingRequestViewResponse cancel(String requestId, RequestScope scope);

    BuddyPostViewResponse close(String postId, RequestScope scope);
}
