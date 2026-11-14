package kz.astyq.zimran.pair.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.i18n.MessageService;
import kz.astyq.zimran.event.mapper.EventMapper;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.dto.EventViewResponse;
import kz.astyq.zimran.event.service.EventQueryService;
import kz.astyq.zimran.pair.mapper.PairMapper;
import kz.astyq.zimran.pair.model.domain.PairAccess;
import kz.astyq.zimran.pair.model.dto.PairCompromiseViewResponse;
import kz.astyq.zimran.pair.model.dto.PairConflictViewResponse;
import kz.astyq.zimran.pair.model.dto.PairSessionViewResponse;
import kz.astyq.zimran.pair.model.dto.PairSharedPlanViewResponse;
import kz.astyq.zimran.pair.model.dto.PairSuggestionViewResponse;
import kz.astyq.zimran.pair.model.entity.PairParticipant;
import kz.astyq.zimran.pair.model.entity.PairSession;
import kz.astyq.zimran.pair.model.entity.PairSuggestion;
import kz.astyq.zimran.pair.model.entity.PairVote;
import kz.astyq.zimran.pair.model.enums.PairConflictType;
import kz.astyq.zimran.pair.model.enums.PairRole;
import kz.astyq.zimran.pair.model.enums.PairStatus;
import kz.astyq.zimran.pair.model.enums.PairVoteType;
import kz.astyq.zimran.pair.repository.PairSuggestionRepository;
import kz.astyq.zimran.pair.repository.PairVoteRepository;
import kz.astyq.zimran.pair.service.PairDetailsService;
import kz.astyq.zimran.pair.util.MessageCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PairDetailsServiceImpl implements PairDetailsService {

    private final PairSuggestionRepository pairSuggestionRepository;
    private final PairVoteRepository pairVoteRepository;
    private final EventQueryService eventQueryService;
    private final SupportedCityService supportedCityService;
    private final EventMapper eventMapper;
    private final PairMapper pairMapper;
    private final MessageService messageService;

    @Override
    @Transactional(readOnly = true)
    public PairSessionViewResponse describe(PairAccess access) {
        PairSession session = access.getSession();
        PairParticipant me = access.getMe();
        PairRole myRole = me.getRole();

        Map<PairRole, Map<String, PairVoteType>> votes = new HashMap<>();
        for (PairVote vote : pairVoteRepository.findBySessionId(session.getId())) {
            votes.computeIfAbsent(vote.getRole(), key -> new HashMap<>()).put(vote.getEventId(), vote.getVote());
        }
        Map<String, PairVoteType> myVotes = votes.getOrDefault(myRole, Map.of());
        Map<String, PairVoteType> partnerVotes = votes.getOrDefault(myRole.other(), Map.of());

        Map<String, EventViewResponse> events = new HashMap<>();
        List<PairSuggestionViewResponse> suggestions = new ArrayList<>();
        PairSharedPlanViewResponse sharedPlan = null;
        for (PairSuggestion suggestion : pairSuggestionRepository.findBySessionIdOrderByRankAsc(session.getId())) {
            Optional<EventViewResponse> event = event(suggestion.getEventId(), events);
            if (event.isEmpty()) {
                continue;
            }
            PairVoteType myVote = myVotes.get(suggestion.getEventId());
            suggestions.add(PairSuggestionViewResponse.builder()
                    .rank(suggestion.getRank())
                    .event(event.get())
                    .reasonA(suggestion.getReasonA())
                    .reasonB(suggestion.getReasonB())
                    .compromise(compromise(suggestion.getCompromise(), myRole))
                    .myVote(myVote)
                    .partnerVote(myVote != null ? partnerVotes.get(suggestion.getEventId()) : null)
                    .build());
            if (session.getStatus() == PairStatus.MATCHED
                    && suggestion.getEventId().equals(session.getMatchedEventId())) {
                sharedPlan = PairSharedPlanViewResponse.builder()
                        .event(event.get())
                        .reasonA(suggestion.getReasonA())
                        .reasonB(suggestion.getReasonB())
                        .build();
            }
        }

        return PairSessionViewResponse.builder()
                .sessionId(session.getPublicId())
                .status(session.getStatus())
                .city(session.getCity())
                .cityName(supportedCityService.findById(session.getCity()).map(SupportedCity::getName).orElse(null))
                .expiresAt(session.getExpiresAt())
                .myRole(myRole)
                .me(pairMapper.toViewResponse(me))
                .partner(access.getPartner() != null ? pairMapper.toPartnerViewResponse(access.getPartner()) : null)
                .suggestions(suggestions)
                .sharedPlan(sharedPlan)
                .conflict(session.getStatus() == PairStatus.NO_MATCH ? conflict(session) : null)
                .build();
    }

    private Optional<EventViewResponse> event(String eventId, Map<String, EventViewResponse> cache) {
        if (cache.containsKey(eventId)) {
            return Optional.of(cache.get(eventId));
        }
        Optional<EventViewResponse> event = eventQueryService.findById(EventId.parse(eventId)).map(this::toView);
        event.ifPresent(view -> cache.put(eventId, view));
        return event;
    }

    private EventViewResponse toView(Event event) {
        return eventMapper.toViewResponse(event);
    }

    private PairCompromiseViewResponse compromise(PairRole favors, PairRole myRole) {
        if (favors == null) {
            return null;
        }
        String key = favors == myRole ? MessageCode.PAIR_COMPROMISE_YOURS : MessageCode.PAIR_COMPROMISE_PARTNER;
        return PairCompromiseViewResponse.builder().favors(favors).message(messageService.getMessage(key)).build();
    }

    private PairConflictViewResponse conflict(PairSession session) {
        PairConflictType type = session.getConflictType();
        PairConflictType relax = session.getConflictRelax();
        return PairConflictViewResponse.builder()
                .type(type)
                .message(type != null ? messageService.getMessage(MessageCode.conflict(type)) : null)
                .relax(relax)
                .relaxMessage(relax != null ? messageService.getMessage(MessageCode.relax(relax)) : null)
                .build();
    }
}
