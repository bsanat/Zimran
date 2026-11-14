package kz.astyq.zimran.pair.model.domain;

import kz.astyq.zimran.pair.model.entity.PairSession;
import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

/**
 * A new session with its one-time secrets (shown to the creator once, never stored in clear).
 */
@Value
@Builder
public class PairSessionCreated {

    @NonNull
    PairSession session;
    @NonNull
    @ToString.Exclude
    String inviteToken;
    @NonNull
    @ToString.Exclude
    String participantToken;
}
