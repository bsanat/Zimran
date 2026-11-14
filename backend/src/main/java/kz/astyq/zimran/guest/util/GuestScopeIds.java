package kz.astyq.zimran.guest.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Guest scope ids: {@code gst_} + 128 random bits in lowercase hex.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GuestScopeIds {

    public static final String PREFIX = "gst_";

    private static final Pattern VALID = Pattern.compile("gst_[0-9a-f]{32}");
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String newScopeId() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return PREFIX + HexFormat.of().formatHex(bytes);
    }

    public static boolean isValid(String scopeId) {
        return scopeId != null && VALID.matcher(scopeId).matches();
    }
}
