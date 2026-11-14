package kz.astyq.zimran.moderation.model.enums;

/**
 * What a report is about.
 */
public enum ReportTargetType {
    /** One Buddy post. */
    POST,
    /** One Buddy request. */
    REQUEST,
    /** The person behind a post or request ({@code targetId} is that post or request). */
    SCOPE
}
