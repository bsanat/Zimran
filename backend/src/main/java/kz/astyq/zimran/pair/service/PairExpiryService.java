package kz.astyq.zimran.pair.service;

import kz.astyq.zimran.pair.model.entity.PairSession;

/**
 * Lazy invite expiry: a session still waiting for B is marked EXPIRED when it is next touched after
 * {@code expiresAt}.
 */
public interface PairExpiryService {

    /**
     * Marks the session EXPIRED (and saves it) when it waits for B and its invite expired.
     *
     * @return whether the session is EXPIRED now
     */
    boolean expireIfDue(PairSession session);
}
