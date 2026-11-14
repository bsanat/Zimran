package kz.astyq.zimran.planner.service.impl;

import kz.astyq.zimran.core.i18n.MessageService;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.planner.model.domain.Candidate;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.model.enums.BudgetFit;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import kz.astyq.zimran.planner.service.PlanExplanationService;
import kz.astyq.zimran.planner.util.MessageCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanExplanationServiceImpl implements PlanExplanationService {

    private final MessageService messageService;

    @Override
    public String reasonFor(Candidate candidate, ValidatedPlanRequest request) {
        List<String> sentences = new ArrayList<>();
        String interests = candidate.getMatchedInterests().stream()
                .sorted()
                .map(interest -> messageService.getMessage(MessageCode.interestName(interest)))
                .collect(Collectors.joining(", "));
        sentences.add(messageService.getMessage(MessageCode.PLAN_REASON_INTEREST, interests));
        if (request.getTimePreference() != TimePreference.ANY && candidate.getTimeOfDay() != null) {
            sentences.add(messageService.getMessage(MessageCode.PLAN_REASON_TIME));
        }
        if (isFree(candidate.getEvent()) && candidate.getBudgetFit() == BudgetFit.WITHIN) {
            sentences.add(messageService.getMessage(MessageCode.PLAN_REASON_FREE));
        } else if (request.getBudgetMax() != null && candidate.getBudgetFit() == BudgetFit.WITHIN) {
            sentences.add(messageService.getMessage(MessageCode.PLAN_REASON_BUDGET));
        }
        return String.join(" ", sentences);
    }

    @Override
    public String summaryFor(int count, ValidatedPlanRequest request) {
        if (count == 0) {
            return messageService.getMessage(MessageCode.PLAN_SUMMARY_EMPTY);
        }
        return messageService.getMessage(MessageCode.PLAN_SUMMARY_FOUND, count, request.getCity().getName());
    }

    @Override
    public String notesNotUsed() {
        return messageService.getMessage(MessageCode.PLAN_NOTES_NOT_USED);
    }

    private static boolean isFree(Event event) {
        return event.isPriceKnown() && event.getPriceMin() != null && event.getPriceMin().signum() == 0
                && (event.getPriceMax() == null || event.getPriceMax().signum() == 0);
    }
}
