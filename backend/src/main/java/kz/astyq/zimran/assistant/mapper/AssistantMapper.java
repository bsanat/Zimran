package kz.astyq.zimran.assistant.mapper;

import kz.astyq.zimran.assistant.model.domain.AssistantChatPreferences;
import kz.astyq.zimran.assistant.model.domain.AssistantFilters;
import kz.astyq.zimran.assistant.model.domain.AssistantRecommendation;
import kz.astyq.zimran.assistant.model.dto.AssistantChatPlanViewResponse;
import kz.astyq.zimran.assistant.model.dto.AssistantChatPreferencesRequest;
import kz.astyq.zimran.assistant.model.dto.AssistantChatPreferencesViewResponse;
import kz.astyq.zimran.assistant.model.dto.AssistantParseViewResponse;
import kz.astyq.zimran.assistant.model.dto.AssistantRecommendViewResponse;
import kz.astyq.zimran.assistant.model.dto.AssistantRecommendationViewResponse;
import kz.astyq.zimran.assistant.model.dto.RelaxationSuggestionViewResponse;
import kz.astyq.zimran.assistant.model.enums.AssistantField;
import kz.astyq.zimran.assistant.model.enums.RelaxationSuggestion;
import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.mapper.EventMapper;
import kz.astyq.zimran.planner.model.domain.Recommendation;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = EventMapper.class)
public interface AssistantMapper {

    @Mapping(target = "city", source = "filters.city.id")
    @Mapping(target = "clarificationQuestion", source = "clarificationQuestion")
    AssistantParseViewResponse toViewResponse(AssistantFilters filters, String clarificationQuestion);

    @Mapping(target = "summary", source = "recommendation.summary")
    @Mapping(target = "recommendations", source = "recommendation.recommendations")
    @Mapping(target = "relaxationSuggestions", source = "relaxationSuggestions")
    AssistantRecommendViewResponse toViewResponse(AssistantRecommendation recommendation,
                                                  List<RelaxationSuggestionViewResponse> relaxationSuggestions);

    /** Every event field comes from the catalog row; only the reason (and score) come from the ranking. */
    @Mapping(target = "eventId", source = "event.id")
    AssistantRecommendationViewResponse toViewResponse(Recommendation recommendation);

    @Mapping(target = "city", source = "city.name")
    @Mapping(target = "startDate", source = "dateFrom")
    @Mapping(target = "endDate", source = "dateTo")
    AssistantChatPreferencesViewResponse toViewResponse(AssistantChatPreferences preferences);

    @Mapping(target = "planId", source = "recommendation.planId")
    @Mapping(target = "summary", source = "recommendation.summary")
    @Mapping(target = "recommendations", source = "recommendation.recommendations")
    @Mapping(target = "relaxationSuggestions", source = "relaxationSuggestions")
    AssistantChatPlanViewResponse toChatPlanViewResponse(AssistantRecommendation recommendation,
                                                         List<RelaxationSuggestionViewResponse> relaxationSuggestions);

    /**
     * The preferences a client sent back for the previous turn; {@code city} is the supported city it names
     * (null when unknown or absent).
     */
    default AssistantChatPreferences toDomain(AssistantChatPreferencesRequest request, SupportedCity city) {
        if (request == null) {
            return AssistantChatPreferences.EMPTY;
        }
        return AssistantChatPreferences.builder()
                .city(city)
                .interests(request.getInterests() == null || request.getInterests().isEmpty() ? Set.of()
                        : Collections.unmodifiableSet(EnumSet.copyOf(request.getInterests())))
                .dateFrom(request.getStartDate())
                .dateTo(request.getEndDate())
                .timePreference(request.getTimePreference())
                .budgetMax(request.getBudgetMax())
                .notes(request.getNotes())
                .build();
    }

    @Mapping(target = "code", source = "suggestion.id")
    @Mapping(target = "message", source = "message")
    RelaxationSuggestionViewResponse toViewResponse(RelaxationSuggestion suggestion, String message);

    default String map(AssistantField field) {
        return field != null ? field.getId() : null;
    }

    default String map(TimePreference timePreference) {
        return timePreference != null ? timePreference.getId() : null;
    }

    default String map(LocalDate date) {
        return date != null ? date.toString() : null;
    }
}
