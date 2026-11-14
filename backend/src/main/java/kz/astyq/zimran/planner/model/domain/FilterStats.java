package kz.astyq.zimran.planner.model.domain;

import lombok.Builder;
import lombok.Value;

/**
 * How many of the {@code total} provider events each planner filter removed (each event counts once, for the
 * first filter that removed it).
 */
@Value
@Builder
public class FilterStats {

    int total;
    int notScheduled;
    int past;
    int outsideRange;
    int noInterestMatch;
    int wrongTimeOfDay;
    int overBudget;
}
