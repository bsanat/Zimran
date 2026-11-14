package kz.astyq.zimran.event.model.domain;

import kz.astyq.zimran.event.model.enums.Interest;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * One fictional sample event of {@code demo/demo-events.json}. Its day is relative to the seeding day so the
 * sample stays upcoming; prices are null when the sample shows an unknown price.
 */
@Value
@Builder
@Jacksonized
public class DemoEventSeed {

    @NonNull
    String id;
    @NonNull
    String title;
    String description;
    /** First = primary interest. */
    @NonNull
    List<Interest> interests;
    String venue;
    String address;
    /** Days after the seeding day (0 = today). */
    int dayOffset;
    @NonNull
    LocalTime time;
    BigDecimal priceMin;
    BigDecimal priceMax;
    String currency;
}
