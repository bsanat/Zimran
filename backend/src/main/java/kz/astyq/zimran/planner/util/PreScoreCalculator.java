package kz.astyq.zimran.planner.util;

import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.planner.model.enums.BudgetFit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;

/**
 * Deterministic 0..100 relevance score used to order candidates before AI ranking:
 * <ul>
 *   <li>interest overlap: 40 x matched / requested, at least 20 when one interest matches;</li>
 *   <li>budget: WITHIN +20, UNKNOWN +5;</li>
 *   <li>time of day matches the preference: +15;</li>
 *   <li>has an image: +5; has a description: +5;</li>
 *   <li>soonness: up to +15, linearly from the first day of the range (15) to the last (0); a one-day range
 *       always gets 15.</li>
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PreScoreCalculator {

    static final double INTEREST_MAX = 40;
    static final double INTEREST_MIN_WITH_MATCH = 20;
    static final int BUDGET_WITHIN = 20;
    static final int BUDGET_UNKNOWN = 5;
    static final int TIME_MATCH = 15;
    static final int IMAGE = 5;
    static final int DESCRIPTION = 5;
    static final double SOONNESS_MAX = 15;

    /**
     * @param requestedInterests the guest's interests; {@code matchedInterests} outside it are ignored
     * @param eventDate          the event's day in the city's timezone
     * @param startDate          first day of the requested range
     * @param endDate            last day of the requested range
     */
    public static int score(Event event, Set<Interest> matchedInterests, Set<Interest> requestedInterests,
                            BudgetFit budgetFit, boolean timeMatches,
                            LocalDate eventDate, LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(event, "event");
        double score = interestScore(matchedInterests, requestedInterests)
                + budgetScore(budgetFit)
                + (timeMatches ? TIME_MATCH : 0)
                + (StringUtils.hasText(event.getImageUrl()) ? IMAGE : 0)
                + (StringUtils.hasText(event.getDescription()) ? DESCRIPTION : 0)
                + soonnessScore(eventDate, startDate, endDate);
        return (int) Math.max(0, Math.min(100, Math.round(score)));
    }

    private static double interestScore(Set<Interest> matched, Set<Interest> requested) {
        if (matched == null || requested == null || requested.isEmpty()) {
            return 0;
        }
        long overlap = matched.stream().filter(requested::contains).count();
        if (overlap == 0) {
            return 0;
        }
        return Math.max(INTEREST_MIN_WITH_MATCH, INTEREST_MAX * overlap / requested.size());
    }

    private static int budgetScore(BudgetFit budgetFit) {
        if (budgetFit == null) {
            return 0;
        }
        return switch (budgetFit) {
            case WITHIN -> BUDGET_WITHIN;
            case UNKNOWN -> BUDGET_UNKNOWN;
            case OVER -> 0;
        };
    }

    private static double soonnessScore(LocalDate eventDate, LocalDate startDate, LocalDate endDate) {
        if (eventDate == null || startDate == null || endDate == null
                || eventDate.isBefore(startDate) || eventDate.isAfter(endDate)) {
            return 0;
        }
        long span = ChronoUnit.DAYS.between(startDate, endDate);
        if (span <= 0) {
            return SOONNESS_MAX;
        }
        long offset = ChronoUnit.DAYS.between(startDate, eventDate);
        return SOONNESS_MAX * (span - offset) / span;
    }
}
