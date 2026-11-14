package kz.astyq.zimran.pair.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

/**
 * A freshly generated secret token: {@code value} is handed to the client once, only {@code hash} is stored.
 */
@Value
@Builder
public class PairToken {

    @NonNull
    @ToString.Exclude
    String value;
    /** SHA-256 of {@code value}, lowercase hex. */
    @NonNull
    String hash;
}
