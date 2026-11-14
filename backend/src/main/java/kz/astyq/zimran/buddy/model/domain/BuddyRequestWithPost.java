package kz.astyq.zimran.buddy.model.domain;

import kz.astyq.zimran.buddy.model.entity.BuddyPost;
import kz.astyq.zimran.buddy.model.entity.BuddyRequest;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * One of the caller's requests with the post it was sent to.
 */
@Value
@Builder
public class BuddyRequestWithPost {

    @NonNull
    BuddyRequest request;
    @NonNull
    BuddyPost post;
}
