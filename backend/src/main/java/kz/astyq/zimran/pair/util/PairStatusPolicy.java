package kz.astyq.zimran.pair.util;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.pair.model.entity.PairSession;
import kz.astyq.zimran.pair.model.enums.PairStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The only place that knows the allowed status transitions of a Pair Planning session:
 * <pre>
 * WAITING_FOR_B -> READY | NO_MATCH | EXPIRED | REVOKED
 * READY         -> MATCHED | NO_MATCH | REVOKED
 * MATCHED, NO_MATCH, EXPIRED, REVOKED: final
 * </pre>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PairStatusPolicy {

    private static final Map<PairStatus, Set<PairStatus>> ALLOWED = new EnumMap<>(PairStatus.class);

    static {
        ALLOWED.put(PairStatus.WAITING_FOR_B,
                EnumSet.of(PairStatus.READY, PairStatus.NO_MATCH, PairStatus.EXPIRED, PairStatus.REVOKED));
        ALLOWED.put(PairStatus.READY, EnumSet.of(PairStatus.MATCHED, PairStatus.NO_MATCH, PairStatus.REVOKED));
        for (PairStatus status : PairStatus.values()) {
            ALLOWED.putIfAbsent(status, EnumSet.noneOf(PairStatus.class));
        }
    }

    public static boolean canTransition(PairStatus from, PairStatus to) {
        return from != null && to != null && ALLOWED.get(from).contains(to);
    }

    /**
     * Sets {@code to} on the session. A transition that is not allowed:
     * {@code ServiceValidationException(CONFLICT, PAIR_STATUS_CONFLICT)} with {@code details.status}.
     */
    public static void transition(PairSession session, PairStatus to) {
        Objects.requireNonNull(session, "session");
        if (!canTransition(session.getStatus(), to)) {
            throw new ServiceValidationException(ErrorCode.CONFLICT, MessageCode.PAIR_STATUS_CONFLICT)
                    .withDetail("status", session.getStatus().name());
        }
        session.setStatus(to);
    }

    /**
     * Whether B may still join.
     */
    public static boolean isJoinable(PairStatus status) {
        return canTransition(status, PairStatus.READY);
    }

    /**
     * Whether votes are accepted (a vote can only lead to MATCHED or NO_MATCH from here).
     */
    public static boolean acceptsVotes(PairStatus status) {
        return canTransition(status, PairStatus.MATCHED);
    }
}
