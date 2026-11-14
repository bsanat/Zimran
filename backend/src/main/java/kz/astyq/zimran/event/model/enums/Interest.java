package kz.astyq.zimran.event.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Interests a guest can pick; the same 18 ids as the frontend. Over the wire an interest is its lowercase
 * {@link #getId() id}. Which source categories count as which interest is configured in
 * {@code app.events-source.category-mapping}.
 */
public enum Interest {
    MUSIC,
    SPORTS,
    FOOD,
    ART,
    THEATER,
    OUTDOORS,
    NIGHTLIFE,
    TECHNOLOGY,
    COMEDY,
    GAMING,
    FAMILY,
    BOOKS,
    AI,
    RUNNING,
    CLIMATE,
    FITNESS,
    WELLNESS,
    CRYPTO;

    @JsonValue
    public String getId() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Interest by id, ignoring case and surrounding whitespace.
     */
    public static Optional<Interest> fromId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String wanted = id.strip();
        return Arrays.stream(values()).filter(interest -> interest.name().equalsIgnoreCase(wanted)).findFirst();
    }

    /**
     * JSON reader: unknown ids fail parsing (400 with the property as {@code field}).
     */
    @JsonCreator
    public static Interest fromJson(String id) {
        return fromId(id).orElseThrow(() -> new IllegalArgumentException("Unknown interest"));
    }
}
