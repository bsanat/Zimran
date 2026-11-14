package kz.astyq.zimran.pair.model.domain;

import kz.astyq.zimran.pair.model.enums.PairRole;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * One suggestion of a match, ready to be stored. {@code eventId} is always a catalog event of the candidate set.
 */
@Value
@Builder
public class PairMatchSuggestion {

    @NonNull
    String eventId;
    /** 1 = best. */
    int rank;
    @NonNull
    String reasonA;
    @NonNull
    String reasonB;
    PairRole compromise;
}
