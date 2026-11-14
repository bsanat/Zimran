package kz.astyq.zimran.planner.model.domain;

import kz.astyq.zimran.event.model.domain.Event;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * One recommended event. The event is always one of the plan's candidates; {@code matchScore} is 0..100.
 */
@Value
@Builder
public class Recommendation {

    @NonNull
    Event event;
    int matchScore;
    @NonNull
    String reason;
}
