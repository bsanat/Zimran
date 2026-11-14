package kz.astyq.zimran.core.configuration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Guest identity ({@code app.guest}). {@code tokenSecret} may be blank so the app boots for local development:
 * a random per-process secret is used then, and tokens stop working after a restart. Never log it.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.guest")
public class GuestProperties {

    static final int MIN_SECRET_BYTES = 32;

    /**
     * HMAC-SHA256 key for guest tokens (env {@code GUEST_TOKEN_SECRET}), at least 32 bytes.
     */
    private String tokenSecret;

    @NotNull
    @DurationMin(minutes = 1)
    private Duration tokenTtl = Duration.ofDays(7);

    /**
     * {@code last_seen_at} is written at most this often per guest.
     */
    @NotNull
    @DurationMin(seconds = 1)
    private Duration lastSeenUpdateInterval = Duration.ofMinutes(5);

    public boolean hasTokenSecret() {
        return StringUtils.hasText(tokenSecret);
    }

    @AssertTrue(message = "app.guest.token-secret must be at least 32 bytes")
    public boolean isTokenSecretLongEnough() {
        return !hasTokenSecret() || tokenSecret.getBytes(StandardCharsets.UTF_8).length >= MIN_SECRET_BYTES;
    }
}
