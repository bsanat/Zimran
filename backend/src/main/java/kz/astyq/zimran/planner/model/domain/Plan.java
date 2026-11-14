package kz.astyq.zimran.planner.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * A finished plan: at most {@code app.planner.max-recommendations} distinct, scheduled events, best first, with
 * texts in the guest's language.
 */
@Value
@Builder
public class Plan {

    @NonNull
    UUID id;
    @NonNull
    String summary;
    @NonNull
    List<Recommendation> recommendations;
    @NonNull
    List<String> suggestions;
    /** Whether the ranking came from the AI (false = rule-based fallback). */
    boolean aiRanked;
}
