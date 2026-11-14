package kz.astyq.zimran.event.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Status of an event; serialized lowercase ({@code scheduled}, {@code cancelled}, {@code postponed},
 * {@code expired}). Sources never send {@link #EXPIRED}: the catalog sets it once every day of the event has passed.
 */
public enum EventStatus {
    SCHEDULED,
    CANCELLED,
    POSTPONED,
    EXPIRED;

    @JsonValue
    public String getId() {
        return name().toLowerCase(Locale.ROOT);
    }
}
