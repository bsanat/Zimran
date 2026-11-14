package kz.astyq.zimran.planner.util;

import kz.astyq.zimran.event.model.enums.Interest;
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
    public static final String END_DATE_BEFORE_START = "end_date_before_start";
    /** Argument {0}: the maximum number of days. */
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

    /** Arguments {0}: the number of events, {1}: the city name. */
    public static final String PLAN_SUMMARY_FOUND = "plan_summary_found";
    public static final String PLAN_SUMMARY_EMPTY = "plan_summary_empty";
    /** Argument {0}: the matched interests, comma-separated. */
    public static final String PLAN_REASON_INTEREST = "plan_reason_interest";
    public static final String PLAN_REASON_TIME = "plan_reason_time";
    public static final String PLAN_REASON_BUDGET = "plan_reason_budget";
    public static final String PLAN_REASON_FREE = "plan_reason_free";
    public static final String PLAN_NOTES_NOT_USED = "plan_notes_not_used";

    private static final String INTEREST_PREFIX = "interest_";

    /**
     * Key of an interest's display name, e.g. {@code interest_music}.
     */
    public static String interestName(Interest interest) {
        return INTEREST_PREFIX + interest.getId();
    }
}
