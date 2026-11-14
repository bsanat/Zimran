package kz.astyq.zimran.pair.util;

import kz.astyq.zimran.pair.model.enums.PairConflictType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Locale;

/**
 * Pair Planning message keys in {@code messages*.properties}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageCode {

    public static final String PAIR_NOT_FOUND = "pair_not_found";
    public static final String PAIR_ALREADY_JOINED = "pair_already_joined";
    public static final String PAIR_INVITE_EXPIRED = "pair_invite_expired";
    public static final String PAIR_CREATOR_CANNOT_JOIN = "pair_creator_cannot_join";
    public static final String PAIR_STATUS_CONFLICT = "pair_status_conflict";
    public static final String PAIR_CREATOR_ONLY = "pair_creator_only";
    public static final String PAIR_EVENT_NOT_SUGGESTED = "pair_event_not_suggested";

    /** Argument {0}: the partner's matched interests, comma-separated. */
    public static final String PAIR_REASON_COMPROMISE = "pair_reason_compromise";
    public static final String PAIR_COMPROMISE_YOURS = "pair_compromise_yours";
    public static final String PAIR_COMPROMISE_PARTNER = "pair_compromise_partner";

    private static final String CONFLICT_PREFIX = "pair_conflict_";
    private static final String RELAX_PREFIX = "pair_relax_";

    /**
     * Key of a conflict's explanation, e.g. {@code pair_conflict_budget}.
     */
    public static String conflict(PairConflictType type) {
        return CONFLICT_PREFIX + type.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Key of the tip to relax a constraint, e.g. {@code pair_relax_dates}.
     */
    public static String relax(PairConflictType type) {
        return RELAX_PREFIX + type.name().toLowerCase(Locale.ROOT);
    }
}
