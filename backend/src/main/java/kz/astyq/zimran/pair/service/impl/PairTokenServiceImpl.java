package kz.astyq.zimran.pair.service.impl;

import kz.astyq.zimran.pair.model.domain.PairToken;
import kz.astyq.zimran.pair.service.PairTokenService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PairTokenServiceImpl implements PairTokenService {

    static final int TOKEN_BYTES = 32;
    static final int PUBLIC_ID_BYTES = 16;
    static final String PUBLIC_ID_PREFIX = "pair_";
    /** A 32-byte base64url token has 43 characters; anything far longer is rejected without hashing. */
    private static final int MAX_TOKEN_LENGTH = 128;
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final SecureRandom random = new SecureRandom();

    @Override
    public PairToken generate() {
        String value = randomBase64(TOKEN_BYTES);
        return PairToken.builder().value(value).hash(hash(value)).build();
    }

    @Override
    public String hash(String token) {
        return HexFormat.of().formatHex(sha256(token.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public boolean matches(String token, String expectedHash) {
        if (token == null || token.isBlank() || token.length() > MAX_TOKEN_LENGTH || expectedHash == null) {
            return false;
        }
        byte[] actual = hash(token.strip()).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(actual, expectedHash.getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public String newPublicId() {
        return PUBLIC_ID_PREFIX + randomBase64(PUBLIC_ID_BYTES);
    }

    private String randomBase64(int bytes) {
        byte[] buffer = new byte[bytes];
        random.nextBytes(buffer);
        return ENCODER.encodeToString(buffer);
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
