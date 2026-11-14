package kz.astyq.zimran.core.model.enums;

/**
 * Who a {@link kz.astyq.zimran.core.model.domain.RequestScope} belongs to.
 */
public enum ScopeKind {
    /** An anonymous guest identified by a signed {@code X-Guest-Token}. */
    GUEST,
    /** A signed-in user (P2). */
    USER
}
