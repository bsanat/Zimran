package kz.astyq.zimran.guest.service;

import kz.astyq.zimran.core.service.RequestScopeVerifier;
import kz.astyq.zimran.guest.model.domain.IssuedGuestToken;

/**
 * Signs and verifies guest tokens ({@code X-Guest-Token}).
 */
public interface GuestTokenService extends RequestScopeVerifier {

    IssuedGuestToken issue(String scopeId);
}
