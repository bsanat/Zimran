package kz.astyq.zimran.pair.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.pair.model.domain.PairAccess;
import kz.astyq.zimran.pair.model.domain.PairJoinTarget;
import kz.astyq.zimran.pair.model.domain.PairMatch;
import kz.astyq.zimran.pair.model.domain.PairSessionCreated;
import kz.astyq.zimran.pair.model.domain.PairSessionJoined;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;

/**
 * Creating, joining and revoking Pair Planning sessions.
 */
public interface PairSessionService {

    /**
     * Counts one use of the Pair quota (free trial or Premium fair use; errors as {@code UsageQuotaService}) and
     * creates a WAITING_FOR_B session with side A, a single-use invite valid for {@code app.pair.invite-ttl} and
     * A's participant token.
     */
    PairSessionCreated create(RequestScope scope, ValidatedPlanRequest creator);

    /**
     * The session the invite opens, checked for joining by {@code scope}: unknown token or revoked session
     * {@code 404}, expired invite {@code 410 PAIR_INVITE_EXPIRED}, already joined {@code 409 PAIR_ALREADY_JOINED},
     * the creator's own scope {@code 409 CONFLICT}.
     */
    PairJoinTarget findJoinTarget(String inviteToken, RequestScope scope);

    /**
     * Adds side B and the match result atomically, re-checking the target like {@link #findJoinTarget} under a
     * row lock (so a second join fails with {@code 409 PAIR_ALREADY_JOINED}). With suggestions the session becomes
     * READY, else NO_MATCH with the conflict.
     */
    PairSessionJoined join(PairJoinTarget target, RequestScope scope, ValidatedPlanRequest invitee, PairMatch match);

    /**
     * Revokes the session. Only the creator (side A): otherwise {@code 403 FORBIDDEN}; a final status:
     * {@code 409 CONFLICT}.
     */
    void revoke(PairAccess access);
}
