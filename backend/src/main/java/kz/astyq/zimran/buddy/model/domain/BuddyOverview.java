package kz.astyq.zimran.buddy.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

/**
 * The caller's Buddy activity: their posts (newest first) and the requests they sent (newest first).
 */
@Value
@Builder
public class BuddyOverview {

    @NonNull
    List<BuddyPostWithRequests> posts;
    @NonNull
    List<BuddyRequestWithPost> requests;
}
