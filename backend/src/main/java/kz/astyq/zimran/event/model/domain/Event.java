package kz.astyq.zimran.event.model.domain;

import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.model.enums.Interest;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Normalized event. Every value comes from the provider's data; optional fields are null when the
 * provider did not send them. {@code description} is untrusted text.
 */
@Value
@Builder
public class Event {

    @NonNull
    EventId id;
    @NonNull
    String title;
    @NonNull
    String sourceUrl;
    /** Start in the venue's timezone. */
    @NonNull
    OffsetDateTime startAt;
    @NonNull
    ZoneId timezone;
    @NonNull
    EventStatus status;

    String description;
    @NonNull
    String category;
    @NonNull
    List<Interest> interests;

    String venue;
    String address;
    String city;

    /** Null when unknown; never 0 unless the provider says 0. */
    BigDecimal priceMin;
    BigDecimal priceMax;
    String currency;
    boolean priceKnown;

    String imageUrl;
}
