package kz.astyq.zimran.ai.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * One event the model chose. {@code eventId} is unverified: the caller must check it against its candidates.
 */
@Value
@Builder
public class AiPick {

    @NonNull
    String eventId;
    /** 0..100. */
    int matchScore;
    /** Sanitized model text; may be empty. */
    @NonNull
    String reason;
}
