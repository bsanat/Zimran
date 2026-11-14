package kz.astyq.zimran.buddy.model.enums;

public enum BuddyRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    /** Withdrawn by the sender, or ended by a block. */
    CANCELLED
}
