package kz.astyq.zimran.core.service;

import kz.astyq.zimran.core.model.domain.RequestScope;

import java.util.Optional;

/**
 * Turns a credential sent in a request header into a {@link RequestScope}. One implementation per kind of
 * credential (guest token now, user session in P2); the request filter asks each in order.
 */
public interface RequestScopeVerifier {

    /**
     * Name of the request header carrying the credential.
     */
    String header();

    /**
     * The scope the credential proves, or empty when it is malformed, forged or expired. Must not throw.
     */
    Optional<RequestScope> verify(String credential);
}
