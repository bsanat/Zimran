package kz.astyq.zimran.event.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;

/**
 * An event as stored in the catalog, with the bookkeeping other features key on. {@code catalogId} is internal and
 * never leaves the backend.
 */
@Value
@Builder
public class StoredEvent {

    @NonNull
    Long catalogId;
    @NonNull
    Event event;
    /** When ingestion first inserted the row. */
    @NonNull
    Instant firstSeenAt;
}
