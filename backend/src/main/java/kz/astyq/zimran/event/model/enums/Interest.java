package kz.astyq.zimran.event.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Interests a guest can pick; the same 18 ids as the frontend. Over the wire an interest is its lowercase
 * {@link #getId() id}. Which source categories count as which interest is configured in
 * {@code app.events-source.category-mapping}. Each interest belongs to one {@link CategorySlug}, the coarser
 * vocabulary shared by the catalog, the AI ranking and alerts.
 */
@Getter
@RequiredArgsConstructor
public enum Interest {
    MUSIC(CategorySlug.MUSIC),
    SPORTS(CategorySlug.SPORTS),
    FOOD(CategorySlug.FOOD_DRINKS),
    ART(CategorySlug.ART),
    THEATER(CategorySlug.THEATER),
    OUTDOORS(CategorySlug.OUTDOORS),
    NIGHTLIFE(CategorySlug.NIGHTLIFE),
    TECHNOLOGY(CategorySlug.TECH),
    COMEDY(CategorySlug.COMEDY),
    GAMING(CategorySlug.OTHER),
    FAMILY(CategorySlug.FAMILY),
    BOOKS(CategorySlug.ART),
    AI(CategorySlug.TECH),
    RUNNING(CategorySlug.SPORTS),
    CLIMATE(CategorySlug.OUTDOORS),
    FITNESS(CategorySlug.SPORTS),
    WELLNESS(CategorySlug.OTHER),
    CRYPTO(CategorySlug.TECH);

    private final CategorySlug categorySlug;

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
