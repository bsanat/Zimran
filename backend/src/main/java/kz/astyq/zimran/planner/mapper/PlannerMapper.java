package kz.astyq.zimran.planner.mapper;

import kz.astyq.zimran.event.mapper.EventMapper;
import kz.astyq.zimran.planner.model.domain.Plan;
import kz.astyq.zimran.planner.model.domain.Recommendation;
import kz.astyq.zimran.planner.model.dto.PlanViewResponse;
import kz.astyq.zimran.planner.model.dto.RecommendationViewResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = EventMapper.class)
public interface PlannerMapper {

    @Mapping(target = "planId", source = "id")
    PlanViewResponse toViewResponse(Plan plan);

    RecommendationViewResponse toViewResponse(Recommendation recommendation);

    default String map(UUID id) {
        return id != null ? id.toString() : null;
    }
}
