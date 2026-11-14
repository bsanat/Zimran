package kz.astyq.zimran.pair.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.configuration.PairProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.pair.mapper.PairMapper;
import kz.astyq.zimran.pair.model.domain.PairAccess;
import kz.astyq.zimran.pair.model.domain.PairJoinTarget;
import kz.astyq.zimran.pair.model.domain.PairMatch;
import kz.astyq.zimran.pair.model.domain.PairSessionCreated;
import kz.astyq.zimran.pair.model.domain.PairSessionJoined;
import kz.astyq.zimran.pair.model.domain.PairToken;
import kz.astyq.zimran.pair.model.entity.PairParticipant;
import kz.astyq.zimran.pair.model.entity.PairSession;
import kz.astyq.zimran.pair.model.enums.PairRole;
import kz.astyq.zimran.pair.model.enums.PairStatus;
import kz.astyq.zimran.pair.repository.PairParticipantRepository;
import kz.astyq.zimran.pair.repository.PairSessionRepository;
import kz.astyq.zimran.pair.repository.PairSuggestionRepository;
import kz.astyq.zimran.pair.service.PairExpiryService;
import kz.astyq.zimran.pair.service.PairSessionService;
import kz.astyq.zimran.pair.service.PairTokenService;
import kz.astyq.zimran.pair.util.MessageCode;
import kz.astyq.zimran.pair.util.PairStatusPolicy;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.subscription.model.enums.UsageFeature;
import kz.astyq.zimran.subscription.service.UsageQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PairSessionServiceImpl implements PairSessionService {

    private final PairSessionRepository pairSessionRepository;
    private final PairParticipantRepository pairParticipantRepository;
    private final PairSuggestionRepository pairSuggestionRepository;
    private final PairTokenService pairTokenService;
    private final PairExpiryService pairExpiryService;
    private final UsageQuotaService usageQuotaService;
    private final SupportedCityService supportedCityService;
    private final PairMapper pairMapper;
    private final PairProperties pairProperties;
    private final Clock clock;

    @Override
    @Transactional
    public PairSessionCreated create(RequestScope scope, ValidatedPlanRequest creator) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(creator, "creator");
        usageQuotaService.consume(scope, UsageFeature.PAIR);

        PairToken invite = pairTokenService.generate();
        PairToken participantToken = pairTokenService.generate();
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        PairSession session = pairSessionRepository.save(PairSession.builder()
                .publicId(pairTokenService.newPublicId())
                .creatorScopeId(scope.getScopeId())
                .inviteTokenHash(invite.getHash())
                .status(PairStatus.WAITING_FOR_B)
                .city(creator.getCity().getId())
                .expiresAt(now.plus(pairProperties.getInviteTtl()))
                .build());
        pairParticipantRepository.save(pairMapper.toEntity(creator, session.getId(), PairRole.A,
                scope.getScopeId(), participantToken.getHash()));
        log.info("Pair session {} created", session.getPublicId());
        return PairSessionCreated.builder()
                .session(session)
                .inviteToken(invite.getValue())
                .participantToken(participantToken.getValue())
                .build();
    }

    @Override
    @Transactional(noRollbackFor = ServiceValidationException.class)
    public PairJoinTarget findJoinTarget(String inviteToken, RequestScope scope) {
        Objects.requireNonNull(scope, "scope");
        if (inviteToken == null || inviteToken.isBlank()) {
            throw notFound();
        }
        PairSession session = pairSessionRepository.findByInviteTokenHash(pairTokenService.hash(inviteToken.strip()))
                .filter(found -> pairTokenService.matches(inviteToken, found.getInviteTokenHash()))
                .orElseThrow(PairSessionServiceImpl::notFound);
        requireJoinable(session, scope);
        SupportedCity city = supportedCityService.findById(session.getCity())
                .orElseThrow(PairSessionServiceImpl::notFound);
        PairParticipant creator = pairParticipantRepository.findBySessionIdOrderByRoleAsc(session.getId()).stream()
                .filter(participant -> participant.getRole() == PairRole.A)
                .findFirst()
                .orElseThrow(PairSessionServiceImpl::notFound);
        return PairJoinTarget.builder()
                .session(session)
                .city(city)
                .creatorPreferences(pairMapper.toPreferences(creator, city))
                .build();
    }

    @Override
    @Transactional(noRollbackFor = ServiceValidationException.class)
    public PairSessionJoined join(PairJoinTarget target, RequestScope scope, ValidatedPlanRequest invitee,
                                  PairMatch match) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(invitee, "invitee");
        Objects.requireNonNull(match, "match");
        PairSession session = pairSessionRepository.findByIdForUpdate(target.getSession().getId())
                .orElseThrow(PairSessionServiceImpl::notFound);
        requireJoinable(session, scope);

        PairToken participantToken = pairTokenService.generate();
        pairParticipantRepository.save(pairMapper.toEntity(invitee, session.getId(), PairRole.B,
                scope.getScopeId(), participantToken.getHash()));
        if (match.isMatched()) {
            pairSuggestionRepository.saveAll(match.getSuggestions().stream()
                    .map(suggestion -> pairMapper.toEntity(suggestion, session.getId()))
                    .toList());
            PairStatusPolicy.transition(session, PairStatus.READY);
        } else {
            PairStatusPolicy.transition(session, PairStatus.NO_MATCH);
            session.setConflictType(match.getConflict());
            session.setConflictRelax(match.getConflict());
        }
        pairSessionRepository.save(session);
        log.info("Pair session {} joined: {} with {} suggestions", session.getPublicId(), session.getStatus(),
                match.getSuggestions().size());
        return PairSessionJoined.builder().session(session).participantToken(participantToken.getValue()).build();
    }

    @Override
    @Transactional
    public void revoke(PairAccess access) {
        Objects.requireNonNull(access, "access");
        if (access.getMe().getRole() != PairRole.A) {
            throw new ServiceValidationException(ErrorCode.FORBIDDEN, MessageCode.PAIR_CREATOR_ONLY);
        }
        PairSession session = pairSessionRepository.findByIdForUpdate(access.getSession().getId())
                .orElseThrow(PairSessionServiceImpl::notFound);
        PairStatusPolicy.transition(session, PairStatus.REVOKED);
        pairSessionRepository.save(session);
        log.info("Pair session {} revoked", session.getPublicId());
    }

    private void requireJoinable(PairSession session, RequestScope scope) {
        if (pairExpiryService.expireIfDue(session)) {
            throw new ServiceValidationException(ErrorCode.PAIR_INVITE_EXPIRED, MessageCode.PAIR_INVITE_EXPIRED);
        }
        if (session.getStatus() == PairStatus.REVOKED) {
            throw notFound();
        }
        if (!PairStatusPolicy.isJoinable(session.getStatus())) {
            throw new ServiceValidationException(ErrorCode.PAIR_ALREADY_JOINED, MessageCode.PAIR_ALREADY_JOINED);
        }
        if (session.getCreatorScopeId().equals(scope.getScopeId())) {
            throw new ServiceValidationException(ErrorCode.CONFLICT, MessageCode.PAIR_CREATOR_CANNOT_JOIN);
        }
    }

    private static ServiceValidationException notFound() {
        return new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.PAIR_NOT_FOUND);
    }
}
