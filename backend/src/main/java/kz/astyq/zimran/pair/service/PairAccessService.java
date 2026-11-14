package kz.astyq.zimran.pair.service;

import kz.astyq.zimran.pair.model.domain.PairAccess;

/**
 * Opens a session with a participant token ({@code X-Pair-Token}).
 */
public interface PairAccessService {

    /**
     * The session and the participant the token belongs to (hashes compared in constant time). Unknown session,
     * missing or wrong token: {@code 404 NOT_FOUND}, the same for all, so the existence of a session is not
     * revealed. An expired invite is marked EXPIRED on the way.
     */
    PairAccess authenticate(String sessionId, String participantToken);
}
