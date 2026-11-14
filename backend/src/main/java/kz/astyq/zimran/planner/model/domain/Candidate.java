package kz.astyq.zimran.planner.model.domain;

import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.planner.model.enums.BudgetFit;
import kz.astyq.zimran.planner.model.enums.TimeOfDay;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Set;

/**
 * An event that passed every planner filter, with the facts used to rank it. {@code preScore} is 0..100.
 */
@Value
@Builder
public class Candidate {

    @NonNull
    Event event;
    @NonNull
    Set<Interest> matchedInterests;
    @NonNull
    BudgetFit budgetFit;
    /** Null when the event has no known start time (only possible with time preference ANY). */
    TimeOfDay timeOfDay;
    int preScore;
}
