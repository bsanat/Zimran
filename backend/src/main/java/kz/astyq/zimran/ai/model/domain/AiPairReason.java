package kz.astyq.zimran.ai.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * The model's reasons for one event. {@code eventId} is unverified: the caller must check it against the events
 * it sent. The texts are sanitized and may be empty.
 */
@Value
@Builder
public class AiPairReason {

    @NonNull
    String eventId;
    @NonNull
    String reasonA;
    @NonNull
    String reasonB;
}
