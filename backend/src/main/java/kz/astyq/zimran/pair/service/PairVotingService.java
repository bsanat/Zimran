package kz.astyq.zimran.pair.service;

import kz.astyq.zimran.pair.model.domain.PairAccess;
import kz.astyq.zimran.pair.model.enums.PairVoteType;

/**
 * Votes on suggested events and the outcome they lead to.
 */
public interface PairVotingService {

    /**
     * Records (or replaces) the caller's vote. Only while READY (else {@code 409 CONFLICT}) and only on a
     * suggested event (else {@code 400 VALIDATION_ERROR}, {@code details.field=eventId}). Then, under a row lock:
     * the first suggestion (by rank) liked by both makes the session MATCHED; both sides having voted on every
     * suggestion without a common LIKE makes it NO_MATCH with conflict VOTES and one constraint to relax.
     *
     * @return the access with the updated session
     */
    PairAccess vote(PairAccess access, String eventId, PairVoteType vote);
}
