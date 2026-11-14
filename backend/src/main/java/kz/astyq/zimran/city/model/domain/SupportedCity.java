package kz.astyq.zimran.city.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.ZoneId;

/**
 * A city the planner supports, with the parameters used to query providers.
 */
@Value
@Builder
public class SupportedCity {

    @NonNull
    String id;
    @NonNull
    String name;
    String stateCode;
    String countryCode;
    @NonNull
    ZoneId zone;
}
