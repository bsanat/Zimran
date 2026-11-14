package kz.astyq.zimran.pair.service;

import kz.astyq.zimran.pair.model.domain.PairCandidate;
import kz.astyq.zimran.pair.model.domain.PairReasons;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;

import java.util.List;
import java.util.Map;

/**
 * Reasons per side for the events chosen by matching. The AI may only phrase them; it never changes which
 * events were chosen.
 */
public interface PairReasonService {

    /**
     * A reason for A and for B for every picked event, keyed by public event id. AI reasons are used only for
     * event ids that were sent (first one per id, non-blank text); every other reason, and all of them when the
     * AI fails, come from i18n templates in the current language.
     *
     * @param combined the request the candidates were built for
     */
    Map<String, PairReasons> reasons(List<PairCandidate> picked, ValidatedPlanRequest combined,
                                     ValidatedPlanRequest a, ValidatedPlanRequest b);
}
