package kz.astyq.zimran.event.model.domain;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import kz.astyq.zimran.event.model.enums.Interest;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * What to look for in the event catalog. Dates are days in the city's timezone, both inclusive. An event with an
 * unknown price never passes {@link #budgetMax} or {@link #freeOnly}.
 */
@Value
@Builder
public class EventCriteria {

    @NonNull
    SupportedCity city;
    /** First day; null = no lower bound. Applies to events with a known date. */
    LocalDate fromDate;
    /** Last day; null = no upper bound. Applies to events with a known date. */
    LocalDate toDate;
    /** Whether events without a known date are kept (they cannot be checked against the bounds). */
    boolean includeUndated;
    /** Empty or null = every interest. */
    Set<Interest> interests;
    /** Case-insensitive substring of title, description or venue; blank = no keyword filter. */
    String keyword;
    /** Only events flagged as major. */
    boolean majorOnly;
    /** Only events of this shared category; null = every category. */
    CategorySlug categorySlug;
    /** Only events whose lowest known price is at most this; null = any price, including unknown. */
    BigDecimal budgetMax;
    /** Only events the source says are free. */
    boolean freeOnly;
    /** Also cancelled and expired events (excluded by default). */
    boolean includeInactive;
}
