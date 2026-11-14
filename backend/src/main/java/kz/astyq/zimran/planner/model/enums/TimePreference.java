package kz.astyq.zimran.planner.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;

/**
 * Time of day the guest asked for; {@link #ANY} accepts every event, the others need a known start time.
 * Over the wire: lowercase ({@code any}, {@code morning}, {@code afternoon}, {@code evening}).
 */
public enum TimePreference {
    ANY,
    MORNING,
    AFTERNOON,
    EVENING;

    @JsonValue
    public String getId() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * JSON reader, ignoring case; unknown values fail parsing (400 with the property as {@code field}).
     */
    @JsonCreator
    public static TimePreference fromJson(String id) {
        String wanted = id != null ? id.strip() : "";
        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(wanted))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown time preference"));
    }
}
