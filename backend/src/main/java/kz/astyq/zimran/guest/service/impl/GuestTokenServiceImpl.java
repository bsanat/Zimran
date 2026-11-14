package kz.astyq.zimran.guest.service.impl;

import kz.astyq.zimran.core.configuration.GuestProperties;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.model.enums.ScopeKind;
import kz.astyq.zimran.core.web.RequestScopeFilter;
import kz.astyq.zimran.guest.model.domain.IssuedGuestToken;
import kz.astyq.zimran.guest.service.GuestTokenService;
import kz.astyq.zimran.guest.util.GuestScopeIds;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

/**
 * Token = {@code v1.<payload>.<signature>}, both parts base64url without padding. The payload is
 * {@code scopeId|issuedAt|expiresAt} (epoch seconds); the signature is HMAC-SHA256 of {@code v1.<payload>} with
 * {@code app.guest.token-secret}. Tokens are stateless: verifying needs no database. Neither tokens nor the secret
 * are ever logged.
 */
@Slf4j
@Service
public class GuestTokenServiceImpl implements GuestTokenService {

    static final String VERSION = "v1";
    private static final String ALGORITHM = "HmacSHA256";
    private static final String SEPARATOR = "|";
    private static final int MAX_TOKEN_LENGTH = 512;
    /** Tolerated clock difference between instances for {@code issuedAt}. */
    private static final long CLOCK_SKEW_SECONDS = 60;
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final GuestProperties properties;
    private final Clock clock;
    private final SecretKeySpec key;

    public GuestTokenServiceImpl(GuestProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.key = new SecretKeySpec(secret(properties), ALGORITHM);
    }

    @Override
    public String header() {
        return RequestScopeFilter.GUEST_TOKEN_HEADER;
    }

    @Override
    public IssuedGuestToken issue(String scopeId) {
        if (!GuestScopeIds.isValid(scopeId)) {
            throw new IllegalArgumentException("Not a guest scope id");
        }
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(properties.getTokenTtl());
        String payload = String.join(SEPARATOR, scopeId,
                String.valueOf(issuedAt.getEpochSecond()), String.valueOf(expiresAt.getEpochSecond()));
        String signed = VERSION + "." + ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String token = signed + "." + ENCODER.encodeToString(sign(signed));
        return IssuedGuestToken.builder()
                .token(token)
                .scopeId(scopeId)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    public Optional<RequestScope> verify(String credential) {
        if (credential == null || credential.length() > MAX_TOKEN_LENGTH) {
            return Optional.empty();
        }
        String[] parts = credential.split("\\.", -1);
        if (parts.length != 3 || !VERSION.equals(parts[0])) {
            return Optional.empty();
        }
        try {
            byte[] expected = sign(parts[0] + "." + parts[1]);
            if (!MessageDigest.isEqual(expected, DECODER.decode(parts[2]))) {
                return Optional.empty();
            }
            String[] fields = new String(DECODER.decode(parts[1]), StandardCharsets.UTF_8).split("\\|", -1);
            if (fields.length != 3 || !GuestScopeIds.isValid(fields[0])) {
                return Optional.empty();
            }
            long issuedAt = Long.parseLong(fields[1]);
            long expiresAt = Long.parseLong(fields[2]);
            long now = clock.instant().getEpochSecond();
            if (now >= expiresAt || issuedAt > now + CLOCK_SKEW_SECONDS) {
                return Optional.empty();
            }
            return Optional.of(RequestScope.builder().scopeId(fields[0]).kind(ScopeKind.GUEST).build());
        } catch (IllegalArgumentException e) {
            // Bad base64 or numbers (NumberFormatException is an IllegalArgumentException).
            return Optional.empty();
        }
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is unavailable", e);
        }
    }

    private static byte[] secret(GuestProperties properties) {
        if (properties.hasTokenSecret()) {
            return properties.getTokenSecret().getBytes(StandardCharsets.UTF_8);
        }
        log.warn("GUEST_TOKEN_SECRET is not set: using a random secret, guest tokens stop working after a restart");
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return random;
    }
}
