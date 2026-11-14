package kz.astyq.zimran.event.model.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Where an event comes from. {@link #getCode()} is the {@code source} field and the prefix of public event ids
 * ({@code json:<sourceEventId>}). {@link #DEMO} marks the fictional sample events seeded when
 * {@code app.demo.enabled=true}; no provider fetches them.
 */
public enum EventSource {
    JSON,
    DEMO;

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
