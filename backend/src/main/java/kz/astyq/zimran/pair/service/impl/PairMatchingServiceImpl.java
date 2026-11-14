package kz.astyq.zimran.pair.service.impl;

import kz.astyq.zimran.core.configuration.PairProperties;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.pair.model.domain.PairCandidate;
import kz.astyq.zimran.pair.model.domain.PairMatch;
import kz.astyq.zimran.pair.model.domain.PairMatchSuggestion;
import kz.astyq.zimran.pair.model.domain.PairReasons;
import kz.astyq.zimran.pair.model.enums.PairConflictType;
import kz.astyq.zimran.pair.model.enums.PairRole;
import kz.astyq.zimran.pair.service.PairConflictService;
import kz.astyq.zimran.pair.service.PairMatchingService;
import kz.astyq.zimran.pair.service.PairReasonService;
import kz.astyq.zimran.planner.model.domain.Candidate;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import kz.astyq.zimran.planner.service.CandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PairMatchingServiceImpl implements PairMatchingService {

    private final CandidateService candidateService;
    private final PairConflictService pairConflictService;
    private final PairReasonService pairReasonService;
    private final PairProperties pairProperties;

    @Override
    public PairMatch match(ValidatedPlanRequest a, ValidatedPlanRequest b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        if (!a.getCity().getId().equals(b.getCity().getId())) {
            throw new IllegalArgumentException("Both sides must be in the same city");
        }
        LocalDate from = a.getStartDate().isAfter(b.getStartDate()) ? a.getStartDate() : b.getStartDate();
        LocalDate to = a.getEndDate().isBefore(b.getEndDate()) ? a.getEndDate() : b.getEndDate();
        Optional<TimePreference> time = intersect(a.getTimePreference(), b.getTimePreference());
        if (from.isAfter(to) || time.isEmpty()) {
            return PairMatch.noMatch(PairConflictType.DATES);
        }

        Set<Interest> union = EnumSet.noneOf(Interest.class);
        union.addAll(a.getInterests());
        union.addAll(b.getInterests());
        ValidatedPlanRequest combined = ValidatedPlanRequest.builder()
                .city(a.getCity())
                .interests(Collections.unmodifiableSet(union))
                .startDate(from)
                .endDate(to)
                .budgetMax(strictBudget(a.getBudgetMax(), b.getBudgetMax()))
                .timePreference(time.get())
                .build();

        List<PairCandidate> picked = pick(candidateService.buildCandidates(combined).getCandidates(), a, b);
        if (picked.isEmpty()) {
            return PairMatch.noMatch(pairConflictService.mostRestrictive(combined, a, b));
        }
        Map<String, PairReasons> reasons = pairReasonService.reasons(picked, combined, a, b);
        List<PairMatchSuggestion> suggestions = new ArrayList<>();
        for (PairCandidate pick : picked) {
            PairReasons reason = reasons.get(pick.eventId());
            suggestions.add(PairMatchSuggestion.builder()
                    .eventId(pick.eventId())
                    .rank(suggestions.size() + 1)
                    .reasonA(reason.getReasonA())
                    .reasonB(reason.getReasonB())
                    .compromise(pick.getCompromise())
                    .build());
        }
        return PairMatch.of(suggestions);
    }

    /**
     * Shared-interest events first (in candidate order, best first); then compromises alternating between the
     * sides, starting with the side whose best compromise scores higher, only up to the minimum.
     */
    private List<PairCandidate> pick(List<Candidate> candidates, ValidatedPlanRequest a, ValidatedPlanRequest b) {
        List<PairCandidate> shared = new ArrayList<>();
        List<PairCandidate> forA = new ArrayList<>();
        List<PairCandidate> forB = new ArrayList<>();
        for (Candidate candidate : candidates) {
            boolean matchesA = !Collections.disjoint(candidate.getMatchedInterests(), a.getInterests());
            boolean matchesB = !Collections.disjoint(candidate.getMatchedInterests(), b.getInterests());
            if (matchesA && matchesB) {
                shared.add(PairCandidate.builder().candidate(candidate).build());
            } else if (matchesA) {
                forA.add(PairCandidate.builder().candidate(candidate).compromise(PairRole.A).build());
            } else if (matchesB) {
                forB.add(PairCandidate.builder().candidate(candidate).compromise(PairRole.B).build());
            }
        }
        int max = pairProperties.getMaxSuggestions();
        List<PairCandidate> picked = new ArrayList<>(shared.subList(0, Math.min(max, shared.size())));
        int target = Math.min(max, pairProperties.getMinSuggestions());
        boolean aFirst = forB.isEmpty() || (!forA.isEmpty()
                && forA.getFirst().getCandidate().getPreScore() >= forB.getFirst().getCandidate().getPreScore());
        Iterator<PairCandidate> first = (aFirst ? forA : forB).iterator();
        Iterator<PairCandidate> second = (aFirst ? forB : forA).iterator();
        while (picked.size() < target && (first.hasNext() || second.hasNext())) {
            if (first.hasNext()) {
                picked.add(first.next());
            }
            if (picked.size() < target && second.hasNext()) {
                picked.add(second.next());
            }
        }
        return picked;
    }

    /**
     * ANY accepts the other side's choice; two different specific times do not overlap.
     */
    private static Optional<TimePreference> intersect(TimePreference x, TimePreference y) {
        if (x == TimePreference.ANY) {
            return Optional.of(y);
        }
        if (y == TimePreference.ANY || x == y) {
            return Optional.of(x);
        }
        return Optional.empty();
    }

    private static BigDecimal strictBudget(BigDecimal x, BigDecimal y) {
        if (x == null) {
            return y;
        }
        if (y == null) {
            return x;
        }
        return x.min(y);
    }
}
