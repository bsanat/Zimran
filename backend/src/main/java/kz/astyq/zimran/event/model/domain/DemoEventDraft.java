package kz.astyq.zimran.event.model.domain;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * A fictional sample event to add to the catalog on request (demo only). Every value is already resolved.
 */
@Value
@Builder
public class DemoEventDraft {

    @NonNull
    CategorySlug categorySlug;
    @NonNull
    SupportedCity city;
    @NonNull
    String title;
    /** In the city's timezone. */
    @NonNull
    OffsetDateTime startAt;
    /** Null = unknown price. */
    BigDecimal priceMax;
}
