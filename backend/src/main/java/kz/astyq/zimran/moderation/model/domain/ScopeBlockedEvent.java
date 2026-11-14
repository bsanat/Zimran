package kz.astyq.zimran.moderation.model.domain;

import lombok.NonNull;
import lombok.Value;

/**
 * Published (in the blocking transaction) when one scope blocks another, so features can end what the two share.
 */
@Value
public class ScopeBlockedEvent {

    @NonNull
    String blockerScopeId;
    @NonNull
    String blockedScopeId;
}
