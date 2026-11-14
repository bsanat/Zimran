package kz.astyq.zimran.ai.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

/**
 * One side of a pair as the model sees it: structured preferences only, never the side's free text (the other
 * side must not learn it through the reasons).
 */
@Value
@Builder
public class AiPairPerson {

    @NonNull
    List<String> interests;
    @NonNull
    String startDate;
    @NonNull
    String endDate;
    @NonNull
    String timeOfDay;
    /** Null = no limit. */
    String budget;
}
