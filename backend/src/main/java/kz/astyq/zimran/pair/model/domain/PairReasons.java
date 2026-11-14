package kz.astyq.zimran.pair.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Why one suggested event suits side A and side B.
 */
@Value
@Builder
public class PairReasons {

    @NonNull
    String reasonA;
    @NonNull
    String reasonB;
}
