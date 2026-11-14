package kz.astyq.zimran.subscription.model.enums;

/**
 * Entitlement of a scope. A scope without a stored entitlement is {@link #FREE}.
 */
public enum SubscriptionStatus {
    FREE,
    /** Premium unlocked by "Try Premium Demo"; no payment behind it. */
    DEMO_PREMIUM,
    /** A paid, active subscription. */
    PREMIUM_ACTIVE;

    public boolean isPremium() {
        return this == DEMO_PREMIUM || this == PREMIUM_ACTIVE;
    }
}
