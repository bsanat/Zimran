package kz.astyq.zimran.pair.model.domain;

import kz.astyq.zimran.pair.model.enums.PairConflictType;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

/**
 * Result of matching both sides: suggestions best first, or none and the {@code conflict} that removed the most
 * events (which is also the constraint to relax).
 */
@Value
@Builder
public class PairMatch {

    @NonNull
    List<PairMatchSuggestion> suggestions;
    /** Set exactly when {@code suggestions} is empty. */
    PairConflictType conflict;

    public static PairMatch of(List<PairMatchSuggestion> suggestions) {
        return PairMatch.builder().suggestions(List.copyOf(suggestions)).build();
    }

    public static PairMatch noMatch(@NonNull PairConflictType conflict) {
        return PairMatch.builder().suggestions(List.of()).conflict(conflict).build();
    }

    public boolean isMatched() {
        return !suggestions.isEmpty();
    }
}
