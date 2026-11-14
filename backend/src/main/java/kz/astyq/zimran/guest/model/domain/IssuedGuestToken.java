package kz.astyq.zimran.guest.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.time.Instant;

/**
 * A freshly signed guest token. {@code token} is a credential: never log it.
 */
@Value
@Builder
public class IssuedGuestToken {

    @NonNull
    @ToString.Exclude
    String token;
    @NonNull
    String scopeId;
    @NonNull
    Instant issuedAt;
    @NonNull
    Instant expiresAt;
}
