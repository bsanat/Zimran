package kz.astyq.zimran.buddy.util;

import kz.astyq.zimran.buddy.model.entity.BuddyPost;
import kz.astyq.zimran.buddy.model.enums.BuddyPostStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BuddyPostStatuses {

    /** Statuses the expiry job ends. */
    public static final Set<BuddyPostStatus> ACTIVE = EnumSet.of(BuddyPostStatus.OPEN, BuddyPostStatus.MATCHED);

    /**
     * The status as it is now: an active post whose event has started is EXPIRED even before the job ran.
     */
    public static BuddyPostStatus effective(BuddyPost post, Instant now) {
        return ACTIVE.contains(post.getStatus()) && !post.getExpiresAt().isAfter(now)
                ? BuddyPostStatus.EXPIRED : post.getStatus();
    }

    public static boolean isOpen(BuddyPost post, Instant now) {
        return effective(post, now) == BuddyPostStatus.OPEN;
    }
}
