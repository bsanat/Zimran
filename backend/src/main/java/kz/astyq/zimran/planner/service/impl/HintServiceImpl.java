package kz.astyq.zimran.planner.service.impl;

import kz.astyq.zimran.core.i18n.MessageService;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.planner.model.domain.FilterStats;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.service.HintService;
import kz.astyq.zimran.planner.util.MessageCode;
import kz.astyq.zimran.event.service.EventQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HintServiceImpl implements HintService {

    public static final int MAX_HINTS = 3;

    private final MessageService messageService;
    private final EventQueryService eventQueryService;

    @Override
    public List<String> buildHints(FilterStats stats, ValidatedPlanRequest request) {
        Objects.requireNonNull(stats, "stats");
        Objects.requireNonNull(request, "request");
        List<String> hints = new ArrayList<>();

        Set<Interest> covered = eventQueryService.coveredInterests(request.getCity());
        request.getInterests().stream()
                .sorted()
                .filter(interest -> !covered.contains(interest))
                .map(interest -> messageService.getMessage(MessageCode.HINT_LOW_COVERAGE,
                        displayName(interest), request.getCity().getName()))
                .forEach(hints::add);

        filterHintsByImpact(stats).stream()
                .map(messageService::getMessage)
                .forEach(hints::add);

        if (hints.isEmpty()) {
            hints.add(messageService.getMessage(MessageCode.HINT_CHANGE_DATE));
        }
        return hints.stream().limit(MAX_HINTS).toList();
    }

    /**
     * Hint keys of the filters that removed at least one event, most removed first; ties keep the order below.
     * Cancelled, past and out-of-range events all point to "change the date".
     */
    private static List<String> filterHintsByImpact(FilterStats stats) {
        List<Map.Entry<String, Integer>> removedBy = List.of(
                Map.entry(MessageCode.HINT_CHANGE_DATE, stats.getNotScheduled() + stats.getPast() + stats.getOutsideRange()),
                Map.entry(MessageCode.HINT_ADD_INTERESTS, stats.getNoInterestMatch()),
                Map.entry(MessageCode.HINT_CHANGE_TIME, stats.getWrongTimeOfDay()),
                Map.entry(MessageCode.HINT_RAISE_BUDGET, stats.getOverBudget()));
        return removedBy.stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Comparator.comparing(Map.Entry<String, Integer>::getValue).reversed())
                .map(Map.Entry::getKey)
                .toList();
    }

    private String displayName(Interest interest) {
        return messageService.getMessage(MessageCode.interestName(interest));
    }
}
