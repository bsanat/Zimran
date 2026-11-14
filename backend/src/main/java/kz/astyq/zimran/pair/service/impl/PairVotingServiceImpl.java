package kz.astyq.zimran.pair.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.pair.mapper.PairMapper;
import kz.astyq.zimran.pair.model.domain.PairAccess;
import kz.astyq.zimran.pair.model.entity.PairSession;
import kz.astyq.zimran.pair.model.entity.PairSuggestion;
import kz.astyq.zimran.pair.model.entity.PairVote;
import kz.astyq.zimran.pair.model.enums.PairConflictType;
import kz.astyq.zimran.pair.model.enums.PairRole;
import kz.astyq.zimran.pair.model.enums.PairStatus;
import kz.astyq.zimran.pair.model.enums.PairVoteType;
import kz.astyq.zimran.pair.repository.PairSessionRepository;
import kz.astyq.zimran.pair.repository.PairSuggestionRepository;
import kz.astyq.zimran.pair.repository.PairVoteRepository;
import kz.astyq.zimran.pair.service.PairConflictService;
import kz.astyq.zimran.pair.service.PairVotingService;
import kz.astyq.zimran.pair.util.MessageCode;
import kz.astyq.zimran.pair.util.PairStatusPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PairVotingServiceImpl implements PairVotingService {

    private final PairSessionRepository pairSessionRepository;
    private final PairSuggestionRepository pairSuggestionRepository;
    private final PairVoteRepository pairVoteRepository;
    private final PairConflictService pairConflictService;
    private final SupportedCityService supportedCityService;
    private final PairMapper pairMapper;

    @Override
    @Transactional
    public PairAccess vote(PairAccess access, String eventId, PairVoteType vote) {
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(vote, "vote");
        PairSession session = pairSessionRepository.findByIdForUpdate(access.getSession().getId())
                .orElseThrow(() -> new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.PAIR_NOT_FOUND));
        if (!PairStatusPolicy.acceptsVotes(session.getStatus())) {
            throw new ServiceValidationException(ErrorCode.CONFLICT, MessageCode.PAIR_STATUS_CONFLICT)
                    .withDetail("status", session.getStatus().name());
        }
        String id = eventId != null ? eventId.strip() : "";
        List<PairSuggestion> suggestions = pairSuggestionRepository.findBySessionIdOrderByRankAsc(session.getId());
        if (suggestions.stream().noneMatch(suggestion -> suggestion.getEventId().equals(id))) {
            throw new ServiceValidationException(ErrorCode.VALIDATION_ERROR, MessageCode.PAIR_EVENT_NOT_SUGGESTED)
                    .withDetail("field", "eventId");
        }

        PairRole role = access.getMe().getRole();
        PairVote stored = pairVoteRepository.findBySessionIdAndRoleAndEventId(session.getId(), role, id)
                .orElseGet(() -> PairVote.builder().sessionId(session.getId()).role(role).eventId(id).build());
        stored.setVote(vote);
        pairVoteRepository.save(stored);

        evaluate(session, suggestions, access);
        return PairAccess.builder().session(session).me(access.getMe()).partner(access.getPartner()).build();
    }

    private void evaluate(PairSession session, List<PairSuggestion> suggestions, PairAccess access) {
        Map<String, Map<PairRole, PairVoteType>> votes = new HashMap<>();
        for (PairVote vote : pairVoteRepository.findBySessionId(session.getId())) {
            votes.computeIfAbsent(vote.getEventId(), key -> new EnumMap<>(PairRole.class))
                    .put(vote.getRole(), vote.getVote());
        }
        for (PairSuggestion suggestion : suggestions) {
            Map<PairRole, PairVoteType> eventVotes = votes.getOrDefault(suggestion.getEventId(), Map.of());
            if (eventVotes.get(PairRole.A) == PairVoteType.LIKE && eventVotes.get(PairRole.B) == PairVoteType.LIKE) {
                PairStatusPolicy.transition(session, PairStatus.MATCHED);
                session.setMatchedEventId(suggestion.getEventId());
                pairSessionRepository.save(session);
                log.info("Pair session {} matched", session.getPublicId());
                return;
            }
        }
        boolean allVoted = suggestions.stream().allMatch(suggestion ->
                votes.getOrDefault(suggestion.getEventId(), Map.of()).size() == PairRole.values().length);
        if (allVoted && access.getPartner() != null) {
            SupportedCity city = supportedCityService.findById(session.getCity())
                    .orElseThrow(() -> new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.PAIR_NOT_FOUND));
            PairStatusPolicy.transition(session, PairStatus.NO_MATCH);
            session.setConflictType(PairConflictType.VOTES);
            session.setConflictRelax(pairConflictService.relaxationAfterVotes(
                    pairMapper.toPreferences(access.getMe(), city), pairMapper.toPreferences(access.getPartner(), city)));
            pairSessionRepository.save(session);
            log.info("Pair session {} ended without a common like", session.getPublicId());
        }
    }
}
