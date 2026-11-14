package kz.astyq.zimran.pair.util;

import kz.astyq.zimran.ai.model.domain.AiPairPerson;
import kz.astyq.zimran.ai.model.domain.AiPairReasonRequest;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.pair.model.domain.PairCandidate;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.util.AiRankingRequestFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Turns a match into the AI reason input. Only structured facts are passed on; the sides' messages never are.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AiPairReasonRequestFactory {

    /**
     * @param combined the request the candidates were built for (intersection of both sides)
     */
    public static AiPairReasonRequest create(List<PairCandidate> picked, ValidatedPlanRequest combined,
                                             ValidatedPlanRequest a, ValidatedPlanRequest b, String language) {
        return AiPairReasonRequest.builder()
                .language(language)
                .city(combined.getCity().getName())
                .personA(person(a))
                .personB(person(b))
                .events(picked.stream()
                        .map(pick -> AiRankingRequestFactory.candidate(pick.getCandidate(), combined))
                        .toList())
                .build();
    }

    private static AiPairPerson person(ValidatedPlanRequest request) {
        return AiPairPerson.builder()
                .interests(request.getInterests().stream().sorted().map(Interest::getId).toList())
                .startDate(request.getStartDate().toString())
                .endDate(request.getEndDate().toString())
                .timeOfDay(request.getTimePreference().getId())
                .budget(AiRankingRequestFactory.budget(request.getBudgetMax()))
                .build();
    }
}
