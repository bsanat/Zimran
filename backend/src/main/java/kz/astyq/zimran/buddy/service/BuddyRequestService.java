package kz.astyq.zimran.buddy.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.buddy.model.domain.BuddyRequestDraft;
import kz.astyq.zimran.buddy.model.domain.BuddyRequestWithPost;
import kz.astyq.zimran.buddy.model.enums.BuddyDecision;

/**
 * Requests to join Buddy posts. Premium and age confirmation are checked by the caller.
 */
public interface BuddyRequestService {

    /**
     * A PENDING request to an OPEN post. Own post or a second request to the same post: {@code 409 CONFLICT};
     * post not open: {@code 409 CONFLICT}; blocked either way: {@code 403 FORBIDDEN}.
     */
    BuddyRequestWithPost create(RequestScope scope, String postId, BuddyRequestDraft draft);

    /**
     * Post owner only ({@code 403 FORBIDDEN}). ACCEPT: the request ACCEPTED, the post MATCHED and its other
     * pending requests DECLINED; DECLINE: the request DECLINED. Not pending or post not open: {@code 409}.
     */
    BuddyRequestWithPost decide(RequestScope scope, String requestId, BuddyDecision decision);

    /**
     * Sender only ({@code 403 FORBIDDEN}); a PENDING or ACCEPTED request becomes CANCELLED. Cancelling the
     * accepted request reopens its MATCHED post while the event has not started.
     */
    BuddyRequestWithPost cancel(RequestScope scope, String requestId);

    /**
     * Cancels the pending and accepted requests between two scopes (after a block), reopening matched posts.
     */
    void cancelBetween(String firstScopeId, String secondScopeId);
}
