package kz.astyq.zimran.planner.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Planner message keys in {@code messages*.properties}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageCode {

    public static final String INTERESTS_REQUIRED = "interests_required";
    public static final String START_DATE_REQUIRED = "start_date_required";
    public static final String START_DATE_IN_PAST = "start_date_in_past";
    /** Argument {0}: the maximum number of days ahead. */
    public static final String START_DATE_TOO_FAR = "start_date_too_far";
    public static final String END_DATE_BEFORE_START = "end_date_before_start";
    /** Argument {0}: the maximum number of days. Same key (and text) as the event catalog's. */
    public static final String DATE_RANGE_TOO_LONG = "date_range_too_long";
    public static final String BUDGET_INVALID = "budget_invalid";
    /** Argument {0}: the maximum number of characters. */
    public static final String NOTES_TOO_LONG = "notes_too_long";

    public static final String HINT_CHANGE_DATE = "hint_change_date";
    public static final String HINT_RAISE_BUDGET = "hint_raise_budget";
    public static final String HINT_ADD_INTERESTS = "hint_add_interests";
    public static final String HINT_CHANGE_TIME = "hint_change_time";
    /** Arguments {0}: the interest, {1}: the city name. */
    public static final String HINT_LOW_COVERAGE = "hint_low_coverage";
}
