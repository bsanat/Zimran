package kz.astyq.zimran.notification.util;

import kz.astyq.zimran.alert.model.enums.AlertFrequency;
import kz.astyq.zimran.notification.model.domain.DeliveryWindow;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/**
 * Delivery windows per alert frequency: INSTANT now; DAILY the next 09:00 in the city's timezone; WEEKLY the next
 * Monday 09:00 there. A window that starts exactly now delivers now. The batch key names the window
 * ({@code daily:2026-11-15}, {@code weekly:2026-11-16}).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DeliveryWindows {

    public static final LocalTime DELIVERY_TIME = LocalTime.of(9, 0);
    public static final String DEMO_BATCH_KEY = "demo";

    public static DeliveryWindow of(AlertFrequency frequency, Instant now, ZoneId zone) {
        return switch (frequency) {
            case INSTANT -> DeliveryWindow.builder().deliverAfter(now).build();
            case DAILY -> window(frequency, nextDaily(now, zone), zone);
            case WEEKLY -> window(frequency, nextWeekly(now, zone), zone);
        };
    }

    /**
     * Immediate delivery whatever the frequency: the documented override of the demo alert check.
     */
    public static DeliveryWindow demo(Instant now) {
        return DeliveryWindow.builder().deliverAfter(now).batchKey(DEMO_BATCH_KEY).build();
    }

    private static LocalDate nextDaily(Instant now, ZoneId zone) {
        LocalDate today = LocalDate.ofInstant(now, zone);
        return startsBefore(today, now, zone) ? today.plusDays(1) : today;
    }

    private static LocalDate nextWeekly(Instant now, ZoneId zone) {
        LocalDate monday = LocalDate.ofInstant(now, zone).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        return startsBefore(monday, now, zone) ? monday.plusWeeks(1) : monday;
    }

    private static boolean startsBefore(LocalDate day, Instant now, ZoneId zone) {
        return day.atTime(DELIVERY_TIME).atZone(zone).toInstant().isBefore(now);
    }

    private static DeliveryWindow window(AlertFrequency frequency, LocalDate day, ZoneId zone) {
        return DeliveryWindow.builder()
                .deliverAfter(day.atTime(DELIVERY_TIME).atZone(zone).toInstant())
                .batchKey(frequency.name().toLowerCase(Locale.ROOT) + ":" + day)
                .build();
    }
}
