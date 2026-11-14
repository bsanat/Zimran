package kz.astyq.zimran.event.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Status of an event; serialized lowercase ({@code scheduled}, {@code cancelled}, {@code postponed}).
 */
public enum EventStatus {
    SCHEDULED,
    CANCELLED,
    POSTPONED;

    @JsonValue
    public String getId() {
        return name().toLowerCase(Locale.ROOT);
    }
}
