package kz.astyq.zimran.alert.model.enums;

/**
 * How often alert notifications are delivered. {@link #DAILY} and {@link #WEEKLY} deliver at 09:00 in the city's
 * timezone (every day / on Mondays).
 */
public enum AlertFrequency {
    INSTANT,
    DAILY,
    WEEKLY
}
