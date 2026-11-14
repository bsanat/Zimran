package kz.astyq.zimran.pair.service;

import kz.astyq.zimran.pair.model.domain.PairToken;

/**
 * Secrets and public ids of Pair Planning. Tokens are 32 random bytes, base64url without padding; only their
 * SHA-256 hashes are stored. Tokens are never logged.
 */
public interface PairTokenService {

    PairToken generate();

    /**
     * SHA-256 of {@code token}, lowercase hex.
     */
    String hash(String token);

    /**
     * Whether {@code token} hashes to {@code expectedHash}, compared in constant time. False for a null, blank or
     * oversized token.
     */
    boolean matches(String token, String expectedHash);

    /**
     * A new public session id, {@code pair_<random>}.
     */
    String newPublicId();
}
