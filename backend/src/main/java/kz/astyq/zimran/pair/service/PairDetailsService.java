package kz.astyq.zimran.pair.service;

import kz.astyq.zimran.pair.model.domain.PairAccess;
import kz.astyq.zimran.pair.model.dto.PairSessionViewResponse;

/**
 * A session as one participant may see it.
 */
public interface PairDetailsService {

    /**
     * Status, own inputs (with message), the other side's inputs without message, suggestions with both reasons
     * and the event data from the catalog, own votes, the other side's vote on an event only once the caller voted
     * on it, the shared plan when MATCHED and the conflict when NO_MATCH. Suggested events the catalog no longer
     * knows are left out (nothing is invented).
     */
    PairSessionViewResponse describe(PairAccess access);
}
