package kz.astyq.zimran.notification.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;

/**
 * When a notification becomes visible, and the batch it is delivered in.
 */
@Value
@Builder
public class DeliveryWindow {

    @NonNull
    Instant deliverAfter;
    /** Null for an immediate, unbatched delivery. */
    String batchKey;
}
