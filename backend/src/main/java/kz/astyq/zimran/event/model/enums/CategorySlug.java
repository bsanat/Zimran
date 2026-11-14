package kz.astyq.zimran.event.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The one category vocabulary of the catalog, the AI ranking and alerts. Every {@link Interest} belongs to exactly
 * one slug ({@link Interest#getCategorySlug()}); an event's slug is the slug of its primary interest. Over the wire a
 * slug is its lowercase {@link #getId() id} ({@code food_drinks}).
 */
public enum CategorySlug {
    MUSIC,
    FOOD_DRINKS,
    SPORTS,
    ART,
    THEATER,
    OUTDOORS,
    NIGHTLIFE,
    TECH,
    COMEDY,
    FAMILY,
    OTHER;

    @JsonValue
    public String getId() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Interests that belong to this slug, in declaration order.
     */
    public Set<Interest> interests() {
        Set<Interest> interests = EnumSet.noneOf(Interest.class);
        Arrays.stream(Interest.values()).filter(interest -> interest.getCategorySlug() == this).forEach(interests::add);
        return interests;
    }

    /**
     * Slug by id, ignoring case and surrounding whitespace.
     */
    public static Optional<CategorySlug> fromId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String wanted = id.strip();
        return Arrays.stream(values()).filter(slug -> slug.name().equalsIgnoreCase(wanted)).findFirst();
    }

    /**
     * JSON reader: unknown ids fail parsing (400 with the property as {@code field}).
     */
    @JsonCreator
    public static CategorySlug fromJson(String id) {
        return fromId(id).orElseThrow(() -> new IllegalArgumentException("Unknown category"));
    }
}
