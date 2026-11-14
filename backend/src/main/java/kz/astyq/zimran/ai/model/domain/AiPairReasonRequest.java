package kz.astyq.zimran.ai.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

/**
 * Input for writing per-side reasons of events already chosen in code. The model does not choose or rank.
 */
@Value
@Builder
public class AiPairReasonRequest {

    /** {@code en}, {@code ru} or {@code kk}. */
    @NonNull
    String language;
    @NonNull
    String city;
    @NonNull
    AiPairPerson personA;
    @NonNull
    AiPairPerson personB;
    @NonNull
    List<AiCandidate> events;
}
