package kz.astyq.zimran.planner.view.service.impl;

import kz.astyq.zimran.core.model.AppSettings;
import kz.astyq.zimran.planner.mapper.PlannerMapper;
import kz.astyq.zimran.planner.model.domain.CandidateSet;
import kz.astyq.zimran.planner.model.domain.Plan;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.model.dto.PlanCreateRequest;
import kz.astyq.zimran.planner.model.dto.PlanViewResponse;
import kz.astyq.zimran.planner.service.CandidateService;
import kz.astyq.zimran.planner.service.PlanRequestValidationService;
import kz.astyq.zimran.planner.service.RecommendationService;
import kz.astyq.zimran.planner.view.service.PlannerViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlannerViewServiceImpl implements PlannerViewService {

    private final PlanRequestValidationService planRequestValidationService;
    private final CandidateService candidateService;
    private final RecommendationService recommendationService;
    private final PlannerMapper plannerMapper;
    private final AppSettings appSettings;

    @Override
    public PlanViewResponse createPlan(PlanCreateRequest request) {
        ValidatedPlanRequest validated = planRequestValidationService.validate(request);
        CandidateSet candidateSet = candidateService.buildCandidates(validated);
        Plan plan = recommendationService.recommend(validated, candidateSet, appSettings.getLocale());
        log.info("Plan {}: {} candidates, {} recommendations, ai={}", plan.getId(),
                candidateSet.getCandidates().size(), plan.getRecommendations().size(), plan.isAiRanked());
        return plannerMapper.toViewResponse(plan);
    }
}
