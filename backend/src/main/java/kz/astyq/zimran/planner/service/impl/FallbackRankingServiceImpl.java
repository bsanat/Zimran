package kz.astyq.zimran.planner.service.impl;

import kz.astyq.zimran.core.configuration.PlannerProperties;
import kz.astyq.zimran.planner.model.domain.CandidateSet;
import kz.astyq.zimran.planner.model.domain.Recommendation;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.service.FallbackRankingService;
import kz.astyq.zimran.planner.service.PlanExplanationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FallbackRankingServiceImpl implements FallbackRankingService {

    private final PlanExplanationService planExplanationService;
    private final PlannerProperties plannerProperties;

    @Override
    public List<Recommendation> rank(CandidateSet candidateSet, ValidatedPlanRequest request) {
        return candidateSet.getCandidates().stream()
                .limit(plannerProperties.getMaxRecommendations())
                .map(candidate -> Recommendation.builder()
                        .event(candidate.getEvent())
                        .matchScore(Math.max(0, Math.min(100, candidate.getPreScore())))
                        .reason(planExplanationService.reasonFor(candidate, request))
                        .build())
                .toList();
    }
}
