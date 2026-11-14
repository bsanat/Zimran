package kz.astyq.zimran.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Random public ids for rows that are addressed by clients ({@code bud_...}, {@code blk_...}): 128 random bits,
 * URL-safe, so they can neither be guessed nor enumerated. Internal ids and scope ids never leave the backend.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PublicIds {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    public static String newId(String prefix) {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return prefix + ENCODER.encodeToString(bytes);
    }
}
