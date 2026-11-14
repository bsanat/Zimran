package kz.astyq.zimran.buddy.model.enums;

public enum BuddyPostStatus {
    /** Takes requests. */
    OPEN,
    /** The owner accepted one request. */
    MATCHED,
    /** Closed by the owner. */
    CLOSED,
    /** The event has started (set by the expiry job). */
    EXPIRED
}
