package kz.astyq.zimran.notification.util;

import kz.astyq.zimran.alert.model.domain.AlertSettings;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.StoredEvent;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.util.EventDays;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Whether a stored event matches one scope's alert preferences. The city and the entitlement are checked by the
 * caller.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AlertMatchRules {

    /**
     * Active preferences; the event first seen at or after {@code enabledAt}; its category among the chosen ones;
     * within the budget (an unknown price never fits a set budget); scheduled (ACTIVE) and still upcoming.
     */
    public static boolean matches(AlertSettings settings, StoredEvent stored, Instant now) {
        Event event = stored.getEvent();
        return settings.isActive()
                && !stored.getFirstSeenAt().isBefore(settings.getEnabledAt())
                && settings.getCategorySlugs().contains(event.getCategorySlug())
                && fitsBudget(event, settings.getBudgetMax())
                && event.getStatus() == EventStatus.SCHEDULED
                && isUpcoming(event, now);
    }

    static boolean fitsBudget(Event event, BigDecimal budgetMax) {
        if (budgetMax == null) {
            return true;
        }
        BigDecimal min = event.getPriceMin();
        return event.isPriceKnown() && min != null && min.compareTo(budgetMax) <= 0;
    }

    /**
     * A single-day event with a start time: it starts after {@code now}. Otherwise its last day, in its own
     * timezone, is today or later. Undated events cannot be checked and do not match.
     */
    static boolean isUpcoming(Event event, Instant now) {
        if (event.getSchedule() == null && event.getStartAt() != null) {
            return event.getStartAt().toInstant().isAfter(now);
        }
        LocalDate lastDay = EventDays.lastDay(event);
        return lastDay != null && !lastDay.isBefore(LocalDate.ofInstant(now, event.getTimezone()));
    }
}
