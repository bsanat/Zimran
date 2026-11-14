package kz.astyq.zimran.pair.model.domain;

import kz.astyq.zimran.pair.model.entity.PairSession;
import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

/**
 * B joined: the session (READY or NO_MATCH) and B's one-time participant token.
 */
@Value
@Builder
public class PairSessionJoined {

    @NonNull
    PairSession session;
    @NonNull
    @ToString.Exclude
    String participantToken;
}
