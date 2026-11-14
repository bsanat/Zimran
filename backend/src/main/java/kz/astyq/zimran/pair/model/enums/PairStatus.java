package kz.astyq.zimran.pair.model.enums;

/**
 * Lifecycle of a Pair Planning session. Allowed transitions live in
 * {@link kz.astyq.zimran.pair.util.PairStatusPolicy} only.
 */
public enum PairStatus {
    /** Created by A; the invite is open. */
    WAITING_FOR_B,
    /** B joined and the suggestions are computed; both sides vote. */
    READY,
    /** Both sides liked the same event. */
    MATCHED,
    /** No event fits both sides, or the votes found no common event. */
    NO_MATCH,
    /** The invite expired before B joined. */
    EXPIRED,
    /** The creator closed the session. */
    REVOKED
}
