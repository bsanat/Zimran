package kz.astyq.zimran.planner.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

/**
 * Candidates for ranking, best first, plus the filter statistics and localized hints for the guest (hints are
 * only filled when there are few candidates).
 */
@Value
@Builder
public class CandidateSet {

    @NonNull
    List<Candidate> candidates;
    @NonNull
    FilterStats stats;
    @NonNull
    List<String> hints;
}
