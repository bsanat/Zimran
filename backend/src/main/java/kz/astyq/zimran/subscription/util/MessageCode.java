package kz.astyq.zimran.subscription.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Subscription message keys in {@code messages*.properties}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageCode {

    public static final String PREMIUM_REQUIRED = "premium_required";
    public static final String FREE_LIMIT_REACHED = "free_limit_reached";
    public static final String FAIR_USE_LIMIT_REACHED = "fair_use_limit_reached";

    private static final String FEATURE_PREFIX = "subscription_feature_";

    /**
     * Key of a plan feature's label, e.g. {@code subscription_feature_pair}.
     */
    public static String planFeature(String featureId) {
        return FEATURE_PREFIX + featureId;
    }
}
