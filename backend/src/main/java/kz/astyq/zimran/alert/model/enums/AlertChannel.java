package kz.astyq.zimran.alert.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Where alert notifications are delivered. Over the wire a channel is its lowercase {@link #getId() id}
 * ({@code in_app}). Only {@link #available} channels can be chosen; {@link #EMAIL} comes later (P2).
 */
@Getter
@RequiredArgsConstructor
public enum AlertChannel {
    IN_APP(true),
    EMAIL(false);

    private final boolean available;

    @JsonValue
    public String getId() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Channel by id or enum name, ignoring case and surrounding whitespace.
     */
    public static Optional<AlertChannel> fromId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String wanted = id.strip();
        return Arrays.stream(values()).filter(channel -> channel.name().equalsIgnoreCase(wanted)).findFirst();
    }

    /**
     * JSON reader: unknown ids fail parsing (400 with the property as {@code field}).
     */
    @JsonCreator
    public static AlertChannel fromJson(String id) {
        return fromId(id).orElseThrow(() -> new IllegalArgumentException("Unknown channel"));
    }
}
