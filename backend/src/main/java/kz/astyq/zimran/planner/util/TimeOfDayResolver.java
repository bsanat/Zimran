package kz.astyq.zimran.planner.util;

import kz.astyq.zimran.planner.model.enums.TimeOfDay;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Maps a start time to a {@link TimeOfDay} and checks it against a {@link TimePreference}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeOfDayResolver {

    private static final int MORNING_FROM_HOUR = 5;
    private static final int AFTERNOON_FROM_HOUR = 12;
    private static final int EVENING_FROM_HOUR = 17;

    /**
     * Part of the day of {@code startAt}, read from its own local time, so pass it in the event's timezone:
     * MORNING 05:00-11:59, AFTERNOON 12:00-16:59, EVENING 17:00-04:59.
     */
    public static TimeOfDay resolve(OffsetDateTime startAt) {
        int hour = Objects.requireNonNull(startAt, "startAt").getHour();
        if (hour >= MORNING_FROM_HOUR && hour < AFTERNOON_FROM_HOUR) {
            return TimeOfDay.MORNING;
        }
        if (hour >= AFTERNOON_FROM_HOUR && hour < EVENING_FROM_HOUR) {
            return TimeOfDay.AFTERNOON;
        }
        return TimeOfDay.EVENING;
    }

    /**
     * Whether {@code timeOfDay} satisfies {@code preference}; {@code ANY} (or null) matches everything.
     */
    public static boolean matches(TimePreference preference, TimeOfDay timeOfDay) {
        Objects.requireNonNull(timeOfDay, "timeOfDay");
        if (preference == null) {
            return true;
        }
        return switch (preference) {
            case ANY -> true;
            case MORNING -> timeOfDay == TimeOfDay.MORNING;
            case AFTERNOON -> timeOfDay == TimeOfDay.AFTERNOON;
            case EVENING -> timeOfDay == TimeOfDay.EVENING;
        };
    }
}
