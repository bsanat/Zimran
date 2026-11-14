package kz.astyq.zimran.alert.model.domain;

import kz.astyq.zimran.alert.model.enums.AlertChannel;
import kz.astyq.zimran.alert.model.enums.AlertFrequency;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * The alert preferences of one scope.
 */
@Value
@Builder
public class AlertSettings {

    static final AlertFrequency DEFAULT_FREQUENCY = AlertFrequency.DAILY;

    @NonNull
    RequestScope scope;
    /** Supported city id. */
    @NonNull
    String cityId;
    @NonNull
    Set<CategorySlug> categorySlugs;
    /** Null = any price; a set budget never matches an unknown price. */
    BigDecimal budgetMax;
    @NonNull
    AlertFrequency frequency;
    @NonNull
    Set<AlertChannel> channels;
    boolean paused;
    /** When alerts were first enabled or last un-paused; only events first seen since then match. */
    Instant enabledAt;

    /**
     * Not paused and enabled at some point.
     */
    public boolean isActive() {
        return !paused && enabledAt != null;
    }

    /**
     * What a scope without saved preferences sees: no categories, any price, daily, in-app.
     */
    public static AlertSettings defaults(RequestScope scope, String cityId) {
        return AlertSettings.builder()
                .scope(scope)
                .cityId(cityId)
                .categorySlugs(EnumSet.noneOf(CategorySlug.class))
                .frequency(DEFAULT_FREQUENCY)
                .channels(EnumSet.of(AlertChannel.IN_APP))
                .build();
    }
}
