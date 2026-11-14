package kz.astyq.zimran.event.model.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Upstream provider an event comes from. {@link #getCode()} is the prefix of public event ids
 * ({@code ticketmaster:<id>}).
 */
public enum EventSource {
    TICKETMASTER;

    public String getCode() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<EventSource> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(source -> source.getCode().equals(code)).findFirst();
    }
}
