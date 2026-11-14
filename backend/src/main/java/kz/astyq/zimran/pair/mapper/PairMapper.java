package kz.astyq.zimran.pair.mapper;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.pair.model.entity.PairParticipant;
import kz.astyq.zimran.pair.model.entity.PairSuggestion;
import kz.astyq.zimran.pair.model.dto.PairParticipantViewResponse;
import kz.astyq.zimran.pair.model.domain.PairMatchSuggestion;
import kz.astyq.zimran.pair.model.enums.PairRole;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PairMapper {

    /**
     * The caller's own side, with the free-text message.
     */
    PairParticipantViewResponse toViewResponse(PairParticipant participant);

    /**
     * The other side: never the free-text message.
     */
    @Mapping(target = "message", ignore = true)
    PairParticipantViewResponse toPartnerViewResponse(PairParticipant participant);

    default PairParticipant toEntity(ValidatedPlanRequest preferences, Long sessionId, PairRole role, String scopeId,
                                     String accessTokenHash) {
        return PairParticipant.builder()
                .sessionId(sessionId)
                .role(role)
                .scopeId(scopeId)
                .accessTokenHash(accessTokenHash)
                .interests(preferences.getInterests().stream().sorted().map(Interest::getId).toList())
                .budgetMax(preferences.getBudgetMax())
                .dateFrom(preferences.getStartDate())
                .dateTo(preferences.getEndDate())
                .timePreference(preferences.getTimePreference())
                .message(preferences.getNotes())
                .build();
    }

    default PairSuggestion toEntity(PairMatchSuggestion suggestion, Long sessionId) {
        return PairSuggestion.builder()
                .sessionId(sessionId)
                .eventId(suggestion.getEventId())
                .rank(suggestion.getRank())
                .reasonA(suggestion.getReasonA())
                .reasonB(suggestion.getReasonB())
                .compromise(suggestion.getCompromise())
                .build();
    }

    /**
     * A side's stored inputs as matching input (they were validated when stored, so they are not re-validated).
     */
    default ValidatedPlanRequest toPreferences(PairParticipant participant, SupportedCity city) {
        Set<Interest> interests = EnumSet.noneOf(Interest.class);
        List<String> ids = participant.getInterests() != null ? participant.getInterests() : List.of();
        ids.stream().map(Interest::fromId).flatMap(Optional::stream).forEach(interests::add);
        return ValidatedPlanRequest.builder()
                .city(city)
                .interests(interests)
                .startDate(participant.getDateFrom())
                .endDate(participant.getDateTo())
                .budgetMax(participant.getBudgetMax())
                .timePreference(participant.getTimePreference() != null
                        ? participant.getTimePreference() : TimePreference.ANY)
                .notes(participant.getMessage())
                .build();
    }
}
