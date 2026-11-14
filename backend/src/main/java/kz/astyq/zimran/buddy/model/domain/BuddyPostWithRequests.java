package kz.astyq.zimran.buddy.model.domain;

import kz.astyq.zimran.buddy.model.entity.BuddyPost;
import kz.astyq.zimran.buddy.model.entity.BuddyRequest;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

/**
 * One of the caller's posts with the requests it received, oldest first.
 */
@Value
@Builder
public class BuddyPostWithRequests {

    @NonNull
    BuddyPost post;
    @NonNull
    List<BuddyRequest> requests;
}
