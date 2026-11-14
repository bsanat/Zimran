package kz.astyq.zimran.alert.model.domain;

import kz.astyq.zimran.alert.model.enums.AlertChannel;
import kz.astyq.zimran.alert.model.enums.AlertFrequency;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Validated new alert preferences.
 */
@Value
@Builder
public class AlertSettingsUpdate {

    /** Supported city id. */
    @NonNull
    String cityId;
    /** At least one. */
    @NonNull
    Set<CategorySlug> categorySlugs;
    BigDecimal budgetMax;
    @NonNull
    AlertFrequency frequency;
    /** Available channels only, at least one. */
    @NonNull
    Set<AlertChannel> channels;
    boolean paused;
}
