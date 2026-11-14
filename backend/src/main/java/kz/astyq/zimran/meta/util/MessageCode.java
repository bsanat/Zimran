package kz.astyq.zimran.meta.util;

import kz.astyq.zimran.event.model.enums.CategorySlug;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Meta message keys in {@code messages*.properties}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageCode {

    private static final String CATEGORY_PREFIX = "category_";

    /**
     * Label key of a category slug, e.g. {@code category_food_drinks}.
     */
    public static String category(CategorySlug slug) {
        return CATEGORY_PREFIX + slug.getId();
    }
}
