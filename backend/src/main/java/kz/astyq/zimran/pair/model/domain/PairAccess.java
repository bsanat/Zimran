package kz.astyq.zimran.pair.model.domain;

import kz.astyq.zimran.pair.model.entity.PairParticipant;
import kz.astyq.zimran.pair.model.entity.PairSession;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * A session opened with a valid participant token: who is asking ({@code me}) and the other side, if joined.
 */
@Value
@Builder
public class PairAccess {

    @NonNull
    PairSession session;
    @NonNull
    PairParticipant me;
    /** Null while waiting for B. */
    PairParticipant partner;
}
