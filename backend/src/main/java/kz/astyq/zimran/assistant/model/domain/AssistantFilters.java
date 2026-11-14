package kz.astyq.zimran.assistant.model.domain;

import kz.astyq.zimran.assistant.model.enums.AssistantField;
import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Filters read from a guest message after server-side validation. Every value is either valid or null; a value
 * that is required for a recommendation (city, interests, dates) but null, or a value the model gave but that is
 * invalid, is listed in {@code missingFields}. {@code notes} is sanitized but untrusted text.
 */
@Value
@Builder
public class AssistantFilters {

    SupportedCity city;
    @NonNull
    Set<CategorySlug> interests;
    LocalDate dateFrom;
    LocalDate dateTo;
    TimePreference timeOfDay;
    BigDecimal budgetMax;
    String notes;
    /** In {@link AssistantField} declaration order, without duplicates. */
    @NonNull
    List<AssistantField> missingFields;
}
