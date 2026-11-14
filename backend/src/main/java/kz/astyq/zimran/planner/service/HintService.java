package kz.astyq.zimran.planner.service;

import kz.astyq.zimran.planner.model.domain.FilterStats;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;

import java.util.List;

/**
 * Localized suggestions for a guest whose plan found few or no events.
 */
public interface HintService {

    /**
     * At most 3 hints: first one per
     * requested interest without provider coverage, then one per filter that removed events, the filter that
     * removed the most first. When neither applies, a generic "change the date" hint.
     */
    List<String> buildHints(FilterStats stats, ValidatedPlanRequest request);
}
