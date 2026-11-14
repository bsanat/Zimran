package kz.astyq.zimran.planner.service.impl;

import kz.astyq.zimran.core.configuration.PlannerProperties;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventCriteria;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.event.service.EventQueryService;
import kz.astyq.zimran.event.util.EventOrdering;
import kz.astyq.zimran.planner.model.domain.Candidate;
import kz.astyq.zimran.planner.model.domain.CandidateSet;
import kz.astyq.zimran.planner.model.domain.FilterStats;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.model.enums.BudgetFit;
import kz.astyq.zimran.planner.model.enums.TimeOfDay;
import kz.astyq.zimran.planner.service.CandidateService;
import kz.astyq.zimran.planner.service.HintService;
import kz.astyq.zimran.planner.util.BudgetEvaluator;
import kz.astyq.zimran.planner.util.PreScoreCalculator;
import kz.astyq.zimran.planner.util.TimeOfDayResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CandidateServiceImpl implements CandidateService {

    static final int MAX_PER_VENUE_PER_DAY = 3;
    static final int HINTS_BELOW = 3;

    private static final Comparator<Candidate> BEST_FIRST = Comparator
            .comparingInt(Candidate::getPreScore).reversed()
            .thenComparing(c -> EventOrdering.sortInstant(c.getEvent()), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(c -> c.getEvent().getTitle(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(c -> c.getEvent().getTitle())
            .thenComparing(c -> c.getEvent().getId().asString());

    private final EventQueryService eventQueryService;
    private final HintService hintService;
    private final PlannerProperties plannerProperties;
    private final Clock clock;

    @Override
    public CandidateSet buildCandidates(ValidatedPlanRequest request) {
        Objects.requireNonNull(request, "request");
        List<Event> events = eventQueryService.search(EventCriteria.builder()
                .city(request.getCity())
                .fromDate(request.getStartDate())
                .toDate(request.getEndDate())
                .includeUndated(false)
                .interests(request.getInterests())
                .build());

        Instant now = clock.instant();
        StatsCounter counter = new StatsCounter();
        List<Candidate> passed = new ArrayList<>();
        for (Event event : events) {
            if (event == null) {
                continue;
            }
            counter.total++;
            Candidate candidate = evaluate(event, request, now, counter);
            if (candidate != null) {
                passed.add(candidate);
            }
        }

        List<Candidate> candidates = limitPerVenuePerDay(passed.stream().sorted(BEST_FIRST).toList(), request)
                .stream()
                .limit(plannerProperties.getMaxCandidates())
                .toList();
        FilterStats stats = counter.toStats();
        List<String> hints = candidates.size() < HINTS_BELOW ? hintService.buildHints(stats, request) : List.of();
        return CandidateSet.builder().candidates(candidates).stats(stats).hints(hints).build();
    }

    /**
     * The candidate for {@code event}, or null after counting the first filter that rejects it.
     */
    private Candidate evaluate(Event event, ValidatedPlanRequest request, Instant now, StatsCounter counter) {
        if (event.getStatus() != EventStatus.SCHEDULED) {
            counter.notScheduled++;
            return null;
        }
        // A multi-day event whose first day has passed may still be on; its schedule decides below.
        if (event.getSchedule() == null && event.getStartAt() != null && event.getStartAt().toInstant().isBefore(now)) {
            counter.past++;
            return null;
        }
        LocalDate eventDate = cityDate(event, request);
        if (eventDate == null) {
            counter.outsideRange++;
            return null;
        }
        Set<Interest> matched = matchedInterests(event, request.getInterests());
        if (matched.isEmpty()) {
            counter.noInterestMatch++;
            return null;
        }
        TimeOfDay timeOfDay = event.getStartAt() == null ? null : TimeOfDayResolver.resolve(
                event.getStartAt().atZoneSameInstant(event.getTimezone()).toOffsetDateTime());
        if (!TimeOfDayResolver.matches(request.getTimePreference(), timeOfDay)) {
            counter.wrongTimeOfDay++;
            return null;
        }
        BudgetFit budgetFit = BudgetEvaluator.evaluate(event, request.getBudgetMax());
        if (budgetFit == BudgetFit.OVER) {
            counter.overBudget++;
            return null;
        }
        boolean timeMatches = timeOfDay != null; // passed the preference filter above, so a known time fits
        int preScore = PreScoreCalculator.score(event, matched, request.getInterests(), budgetFit, timeMatches,
                eventDate, request.getStartDate(), request.getEndDate());
        return Candidate.builder()
                .event(event)
                .matchedInterests(matched)
                .budgetFit(budgetFit)
                .timeOfDay(timeOfDay)
                .preScore(preScore)
                .build();
    }

    private static Set<Interest> matchedInterests(Event event, Set<Interest> requested) {
        Set<Interest> matched = EnumSet.noneOf(Interest.class);
        event.getInterests().stream().filter(Objects::nonNull).filter(requested::contains).forEach(matched::add);
        return Collections.unmodifiableSet(matched);
    }

    /**
     * Keeps at most {@link #MAX_PER_VENUE_PER_DAY} events per venue and city day, in the given order. Events
     * without a venue are not limited.
     */
    private static List<Candidate> limitPerVenuePerDay(List<Candidate> sorted, ValidatedPlanRequest request) {
        Map<String, Integer> perVenueDay = new HashMap<>();
        List<Candidate> kept = new ArrayList<>();
        for (Candidate candidate : sorted) {
            String venue = candidate.getEvent().getVenue();
            if (!StringUtils.hasText(venue)) {
                kept.add(candidate);
                continue;
            }
            String key = venue.strip().toLowerCase(Locale.ROOT) + '|' + cityDate(candidate.getEvent(), request);
            if (perVenueDay.merge(key, 1, Integer::sum) <= MAX_PER_VENUE_PER_DAY) {
                kept.add(candidate);
            }
        }
        return kept;
    }

    /**
     * The event's first day within the requested dates, in the city's timezone (the zone request dates are
     * interpreted in); null when it is on none of them or its date is unknown.
     */
    private static LocalDate cityDate(Event event, ValidatedPlanRequest request) {
        return event.firstDayBetween(request.getStartDate(), request.getEndDate(), request.getCity().getZone());
    }

    private static final class StatsCounter {
        private int total;
        private int notScheduled;
        private int past;
        private int outsideRange;
        private int noInterestMatch;
        private int wrongTimeOfDay;
        private int overBudget;

        private FilterStats toStats() {
            return FilterStats.builder()
                    .total(total)
                    .notScheduled(notScheduled)
                    .past(past)
                    .outsideRange(outsideRange)
                    .noInterestMatch(noInterestMatch)
                    .wrongTimeOfDay(wrongTimeOfDay)
                    .overBudget(overBudget)
                    .build();
        }
    }
}
