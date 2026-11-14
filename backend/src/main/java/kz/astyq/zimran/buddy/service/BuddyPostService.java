package kz.astyq.zimran.buddy.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.buddy.model.domain.BuddyPostDraft;
import kz.astyq.zimran.buddy.model.entity.BuddyPost;

/**
 * Creating and closing Buddy posts. Premium and age confirmation are checked by the caller.
 */
public interface BuddyPostService {

    /**
     * An OPEN post for an upcoming event that expires at the event's start. Contact data in the text, or an age
     * range outside 18+: {@code 400}; another OPEN post of the scope for this event: {@code 409 CONFLICT}.
     */
    BuddyPost create(RequestScope scope, BuddyPostDraft draft);

    /**
     * Owner only ({@code 403 FORBIDDEN}); an OPEN or MATCHED post becomes CLOSED and its pending requests DECLINED.
     */
    BuddyPost close(RequestScope scope, String postId);
}
