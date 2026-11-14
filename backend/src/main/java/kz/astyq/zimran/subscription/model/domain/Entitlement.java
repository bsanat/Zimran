package kz.astyq.zimran.subscription.model.domain;

import kz.astyq.zimran.subscription.model.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;

/**
 * The effective entitlement of a scope.
 */
@Value
@Builder
public class Entitlement {

    public static final Entitlement FREE = Entitlement.builder().status(SubscriptionStatus.FREE).build();

    @NonNull
    SubscriptionStatus status;
    boolean demoOnly;
    /** End of the paid period; null when unknown or not applicable. */
    Instant periodEnd;

    public boolean isPremium() {
        return status.isPremium();
    }
}
