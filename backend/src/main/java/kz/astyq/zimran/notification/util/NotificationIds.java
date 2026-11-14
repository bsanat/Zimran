package kz.astyq.zimran.notification.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Public notification ids: {@code notif_} + 128 random bits, base64url without padding.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NotificationIds {

    public static final String PREFIX = "notif_";

    private static final int RANDOM_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    public static String newPublicId() {
        byte[] bytes = new byte[RANDOM_BYTES];
        RANDOM.nextBytes(bytes);
        return PREFIX + ENCODER.encodeToString(bytes);
    }
}
