package kz.astyq.zimran.pair.service.impl;

import kz.astyq.zimran.ai.model.domain.AiPairReason;
import kz.astyq.zimran.ai.service.AiPairReasonService;
import kz.astyq.zimran.core.i18n.MessageService;
import kz.astyq.zimran.core.model.AppSettings;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.pair.model.domain.PairCandidate;
import kz.astyq.zimran.pair.model.domain.PairReasons;
import kz.astyq.zimran.pair.service.PairReasonService;
import kz.astyq.zimran.pair.util.AiPairReasonRequestFactory;
import kz.astyq.zimran.pair.util.MessageCode;
import kz.astyq.zimran.planner.model.domain.Candidate;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.service.PlanExplanationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PairReasonServiceImpl implements PairReasonService {

    private final AiPairReasonService aiPairReasonService;
    private final PlanExplanationService planExplanationService;
    private final MessageService messageService;
    private final AppSettings appSettings;

    @Override
    public Map<String, PairReasons> reasons(List<PairCandidate> picked, ValidatedPlanRequest combined,
                                            ValidatedPlanRequest a, ValidatedPlanRequest b) {
        Map<String, PairReasons> reasons = new LinkedHashMap<>();
        for (PairCandidate pick : picked) {
            reasons.put(pick.eventId(), PairReasons.builder()
                    .reasonA(templateReason(pick.getCandidate(), a, b))
                    .reasonB(templateReason(pick.getCandidate(), b, a))
                    .build());
        }
        aiPairReasonService.explain(AiPairReasonRequestFactory.create(picked, combined, a, b, appSettings.getLocale()))
                .ifPresent(aiReasons -> applyAi(aiReasons, reasons));
        return reasons;
    }

    /**
     * Whitelist: only ids we sent, the first answer per id, and only non-blank texts replace the templates.
     */
    private static void applyAi(List<AiPairReason> aiReasons, Map<String, PairReasons> reasons) {
        Set<String> seen = new HashSet<>();
        Map<String, PairReasons> updates = new HashMap<>();
        for (AiPairReason aiReason : aiReasons) {
            PairReasons template = reasons.get(aiReason.getEventId());
            if (template == null || !seen.add(aiReason.getEventId())) {
                continue;
            }
            updates.put(aiReason.getEventId(), PairReasons.builder()
                    .reasonA(aiReason.getReasonA().isBlank() ? template.getReasonA() : aiReason.getReasonA())
                    .reasonB(aiReason.getReasonB().isBlank() ? template.getReasonB() : aiReason.getReasonB())
                    .build());
        }
        reasons.putAll(updates);
    }

    /**
     * The planner's reason for the interests of {@code side} the event matches; a compromise reason naming the
     * partner's matched interests when it matches none of them.
     */
    private String templateReason(Candidate candidate, ValidatedPlanRequest side, ValidatedPlanRequest partner) {
        Set<Interest> own = intersect(candidate.getMatchedInterests(), side.getInterests());
        if (!own.isEmpty()) {
            Candidate forSide = Candidate.builder()
                    .event(candidate.getEvent())
                    .matchedInterests(own)
                    .budgetFit(candidate.getBudgetFit())
                    .timeOfDay(candidate.getTimeOfDay())
                    .preScore(candidate.getPreScore())
                    .build();
            return planExplanationService.reasonFor(forSide, side);
        }
        String partnerInterests = intersect(candidate.getMatchedInterests(), partner.getInterests()).stream()
                .sorted()
                .map(interest -> messageService.getMessage(
                        kz.astyq.zimran.planner.util.MessageCode.interestName(interest)))
                .collect(Collectors.joining(", "));
        return messageService.getMessage(MessageCode.PAIR_REASON_COMPROMISE, partnerInterests);
    }

    private static Set<Interest> intersect(Set<Interest> matched, Set<Interest> wanted) {
        Set<Interest> result = EnumSet.noneOf(Interest.class);
        matched.stream().filter(wanted::contains).forEach(result::add);
        return Collections.unmodifiableSet(result);
    }
}
