package kz.astyq.zimran.pair.model.domain;

import kz.astyq.zimran.pair.model.enums.PairRole;
import kz.astyq.zimran.planner.model.domain.Candidate;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * A candidate chosen for both sides. {@code compromise} is the side whose interest the event serves when only one
 * side's interests match; null when it matches an interest of both.
 */
@Value
@Builder
public class PairCandidate {

    @NonNull
    Candidate candidate;
    PairRole compromise;

    public String eventId() {
        return candidate.getEvent().getId().asString();
    }
}
