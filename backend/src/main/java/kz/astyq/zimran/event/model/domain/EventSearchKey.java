package kz.astyq.zimran.event.model.domain;

import lombok.NonNull;
import lombok.Value;

import java.time.Instant;

/**
 * Cache key of one provider search: city, time window and classification ({@code null} = all).
 */
@Value
public class EventSearchKey {

    @NonNull
    String cityId;
    @NonNull
    Instant start;
    @NonNull
    Instant end;
    String classificationName;
}
