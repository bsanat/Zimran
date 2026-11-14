package kz.astyq.zimran.moderation.model.enums;

/**
 * An item another scope wrote, which can be reported or whose author can be blocked.
 */
public enum ModerationTargetKind {
    /** A Buddy post (its author is the post owner). */
    POST,
    /** A Buddy request (its author is the sender). */
    REQUEST
}
