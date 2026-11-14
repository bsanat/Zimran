package kz.astyq.zimran.ai.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

/**
 * Filters the model read from a guest message: schema-checked and sanitized, but every value is still
 * unverified (an unsupported city, an unknown category, a past or malformed date, a negative budget). The caller
 * validates each field. Null = the model found nothing for it.
 */
@Value
@Builder
public class AiExtraction {

    String city;
    String state;
    @NonNull
    List<String> interests;
    /** Expected {@code YYYY-MM-DD}. */
    String dateFrom;
    /** Expected {@code YYYY-MM-DD}. */
    String dateTo;
    /** Expected {@code morning}, {@code afternoon}, {@code evening} or {@code any}. */
    String timeOfDay;
    BigDecimal budgetMax;
    String notes;
}
