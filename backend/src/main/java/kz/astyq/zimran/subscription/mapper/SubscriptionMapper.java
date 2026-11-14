package kz.astyq.zimran.subscription.mapper;

import kz.astyq.zimran.core.configuration.SubscriptionProperties;
import kz.astyq.zimran.subscription.model.domain.Entitlement;
import kz.astyq.zimran.subscription.model.dto.SubscriptionLimitsViewResponse;
import kz.astyq.zimran.subscription.model.dto.SubscriptionPlanViewResponse;
import kz.astyq.zimran.subscription.model.dto.SubscriptionViewResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SubscriptionMapper {

    @Mapping(target = "status", source = "entitlement.status")
    @Mapping(target = "isPremium", source = "entitlement.premium")
    @Mapping(target = "demoOnly", source = "entitlement.demoOnly")
    @Mapping(target = "periodEnd", source = "entitlement.periodEnd")
    @Mapping(target = "plan", source = "plan")
    @Mapping(target = "limits", source = "limits")
    SubscriptionViewResponse toViewResponse(Entitlement entitlement, SubscriptionPlanViewResponse plan,
                                            SubscriptionLimitsViewResponse limits);

    @Mapping(target = "priceCents", source = "plan.priceCents")
    @Mapping(target = "currency", source = "plan.currency")
    @Mapping(target = "interval", source = "plan.interval")
    @Mapping(target = "features", source = "featureLabels")
    SubscriptionPlanViewResponse toPlanViewResponse(SubscriptionProperties.Plan plan, List<String> featureLabels);
}
