package kz.astyq.zimran.pair.model.domain;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.pair.model.entity.PairSession;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * A session B may join, with its city and the creator's preferences (input for matching).
 */
@Value
@Builder
public class PairJoinTarget {

    @NonNull
    PairSession session;
    @NonNull
    SupportedCity city;
    @NonNull
    ValidatedPlanRequest creatorPreferences;
}
