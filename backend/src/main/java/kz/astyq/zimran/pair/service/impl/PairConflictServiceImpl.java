package kz.astyq.zimran.pair.service.impl;

import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.pair.model.enums.PairConflictType;
import kz.astyq.zimran.pair.service.PairConflictService;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import kz.astyq.zimran.planner.service.CandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;

@Service
@RequiredArgsConstructor
public class PairConflictServiceImpl implements PairConflictService {

    private final CandidateService candidateService;

    @Override
    public PairConflictType mostRestrictive(ValidatedPlanRequest combined, ValidatedPlanRequest a,
                                            ValidatedPlanRequest b) {
        int withoutBudget = combined.getBudgetMax() == null ? 0
                : count(combined.toBuilder().budgetMax(null).build());
        int widerDates = count(combined.toBuilder()
                .startDate(min(a.getStartDate(), b.getStartDate()))
                .endDate(max(a.getEndDate(), b.getEndDate()))
                .timePreference(TimePreference.ANY)
                .build());
        int everyInterest = count(combined.toBuilder().interests(EnumSet.allOf(Interest.class)).build());

        PairConflictType result = PairConflictType.DATES;
        int best = 0;
        if (withoutBudget > best) {
            result = PairConflictType.BUDGET;
            best = withoutBudget;
        }
        if (widerDates > best) {
            result = PairConflictType.DATES;
            best = widerDates;
        }
        if (everyInterest > best) {
            result = PairConflictType.INTERESTS;
        }
        return result;
    }

    @Override
    public PairConflictType relaxationAfterVotes(ValidatedPlanRequest a, ValidatedPlanRequest b) {
        if (a.getBudgetMax() != null || b.getBudgetMax() != null) {
            return PairConflictType.BUDGET;
        }
        if (Collections.disjoint(a.getInterests(), b.getInterests())) {
            return PairConflictType.INTERESTS;
        }
        return PairConflictType.DATES;
    }

    private int count(ValidatedPlanRequest request) {
        return candidateService.buildCandidates(request).getCandidates().size();
    }

    private static LocalDate min(LocalDate x, LocalDate y) {
        return x.isBefore(y) ? x : y;
    }

    private static LocalDate max(LocalDate x, LocalDate y) {
        return x.isAfter(y) ? x : y;
    }
}
