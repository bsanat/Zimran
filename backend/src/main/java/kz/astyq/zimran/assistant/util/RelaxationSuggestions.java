package kz.astyq.zimran.assistant.util;

import kz.astyq.zimran.assistant.model.enums.RelaxationSuggestion;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Ways to loosen a request that matched nothing, in a fixed order: raise the budget (only when one was set),
 * widen the dates (always possible), add interests (unless every interest is already selected).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RelaxationSuggestions {

    public static List<RelaxationSuggestion> forRequest(ValidatedPlanRequest request) {
        List<RelaxationSuggestion> suggestions = new ArrayList<>();
        if (request.getBudgetMax() != null) {
            suggestions.add(RelaxationSuggestion.INCREASE_BUDGET);
        }
        suggestions.add(RelaxationSuggestion.WIDEN_DATES);
        if (request.getInterests().size() < Interest.values().length) {
            suggestions.add(RelaxationSuggestion.MORE_INTERESTS);
        }
        return List.copyOf(suggestions);
    }
}
