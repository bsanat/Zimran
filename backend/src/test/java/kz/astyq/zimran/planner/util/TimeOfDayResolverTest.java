package kz.astyq.zimran.planner.util;

import kz.astyq.zimran.planner.model.enums.TimeOfDay;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeOfDayResolverTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "00:00, MORNING",
                "04:59, MORNING",
                "11:59, MORNING",
                "12:00, AFTERNOON",
                "16:59, AFTERNOON",
                "17:00, EVENING",
                "23:59, EVENING"
        })
        void boundaries(String time, TimeOfDay expected) {
            OffsetDateTime startAt = LocalDateTime.parse("2026-11-20T" + time).atZone(NEW_YORK).toOffsetDateTime();

            assertThat(TimeOfDayResolver.resolve(startAt)).isEqualTo(expected);
        }

        @Test
        @DisplayName("uses the local time of the given offset, not UTC")
        void usesLocalTime() {
            // 15:00 UTC is 10:00 in New York
            OffsetDateTime utc = OffsetDateTime.of(2026, 11, 20, 15, 0, 0, 0, ZoneOffset.UTC);

            assertThat(TimeOfDayResolver.resolve(utc)).isEqualTo(TimeOfDay.AFTERNOON);
            assertThat(TimeOfDayResolver.resolve(utc.atZoneSameInstant(NEW_YORK).toOffsetDateTime()))
                    .isEqualTo(TimeOfDay.MORNING);
        }

        @Test
        void rejectsNull() {
            assertThatThrownBy(() -> TimeOfDayResolver.resolve(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("matches")
    class Matches {

        @ParameterizedTest
        @EnumSource(TimeOfDay.class)
        @DisplayName("ANY and null match every time of day")
        void anyMatchesEverything(TimeOfDay timeOfDay) {
            assertThat(TimeOfDayResolver.matches(TimePreference.ANY, timeOfDay)).isTrue();
            assertThat(TimeOfDayResolver.matches(null, timeOfDay)).isTrue();
        }

        @ParameterizedTest(name = "{0} vs {1} -> {2}")
        @CsvSource({
                "MORNING, MORNING, true",
                "MORNING, AFTERNOON, false",
                "MORNING, EVENING, false",
                "AFTERNOON, AFTERNOON, true",
                "AFTERNOON, MORNING, false",
                "EVENING, EVENING, true",
                "EVENING, AFTERNOON, false"
        })
        void specificPreference(TimePreference preference, TimeOfDay timeOfDay, boolean expected) {
            assertThat(TimeOfDayResolver.matches(preference, timeOfDay)).isEqualTo(expected);
        }

        @ParameterizedTest
        @EnumSource(TimePreference.class)
        @DisplayName("an unknown time (null) only matches ANY")
        void unknownTime(TimePreference preference) {
            assertThat(TimeOfDayResolver.matches(preference, null)).isEqualTo(preference == TimePreference.ANY);
        }
    }
}
