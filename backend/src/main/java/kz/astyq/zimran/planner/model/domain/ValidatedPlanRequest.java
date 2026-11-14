package kz.astyq.zimran.planner.model.domain;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * A plan request that passed validation, with defaults applied. {@code notes} is sanitized but still
 * untrusted text; null when the guest wrote nothing.
 */
@Value
@Builder
public class ValidatedPlanRequest {

    @NonNull
    SupportedCity city;
    @NonNull
    Set<Interest> interests;
    @NonNull
    LocalDate startDate;
    @NonNull
    LocalDate endDate;
    /** Null = no budget limit. */
    BigDecimal budgetMax;
    @NonNull
    TimePreference timePreference;
    String notes;
}
