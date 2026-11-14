package kz.astyq.zimran.buddy.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.buddy.model.domain.BuddyOverview;
import kz.astyq.zimran.buddy.model.entity.BuddyPost;

import java.util.List;

public interface BuddyQueryService {

    /**
     * OPEN posts of the event whose event has not started, newest first. With a viewer, posts of scopes the
     * viewer blocked or was blocked by are hidden.
     *
     * @param viewer the caller's scope, or null
     */
    List<BuddyPost> openPosts(String eventId, RequestScope viewer);

    /**
     * The caller's latest posts with their requests and the caller's latest requests with their posts.
     */
    BuddyOverview overview(RequestScope scope);
}
