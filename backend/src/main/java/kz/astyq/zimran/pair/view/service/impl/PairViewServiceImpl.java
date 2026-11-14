package kz.astyq.zimran.pair.view.service.impl;

import kz.astyq.zimran.core.configuration.FrontendProperties;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.model.enums.RateLimitAction;
import kz.astyq.zimran.core.service.RateLimitService;
import kz.astyq.zimran.pair.model.domain.PairAccess;
import kz.astyq.zimran.pair.model.domain.PairJoinTarget;
import kz.astyq.zimran.pair.model.domain.PairMatch;
import kz.astyq.zimran.pair.model.domain.PairSessionCreated;
import kz.astyq.zimran.pair.model.domain.PairSessionJoined;
import kz.astyq.zimran.pair.model.dto.PairJoinCreateRequest;
import kz.astyq.zimran.pair.model.dto.PairJoinViewResponse;
import kz.astyq.zimran.pair.model.dto.PairSessionCreateRequest;
import kz.astyq.zimran.pair.model.dto.PairSessionCreateViewResponse;
import kz.astyq.zimran.pair.model.dto.PairSessionViewResponse;
import kz.astyq.zimran.pair.model.dto.PairVoteCreateRequest;
import kz.astyq.zimran.pair.service.PairAccessService;
import kz.astyq.zimran.pair.service.PairDetailsService;
import kz.astyq.zimran.pair.service.PairMatchingService;
import kz.astyq.zimran.pair.service.PairPreferencesValidationService;
import kz.astyq.zimran.pair.service.PairSessionService;
import kz.astyq.zimran.pair.service.PairVotingService;
import kz.astyq.zimran.pair.view.service.PairViewService;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PairViewServiceImpl implements PairViewService {

    /** The invite token goes in the fragment, so it never reaches server logs or Referer headers. */
    static final String INVITE_PATH = "/pair/join#invite=";

    private final RateLimitService rateLimitService;
    private final PairPreferencesValidationService pairPreferencesValidationService;
    private final PairSessionService pairSessionService;
    private final PairMatchingService pairMatchingService;
    private final PairAccessService pairAccessService;
    private final PairVotingService pairVotingService;
    private final PairDetailsService pairDetailsService;
    private final FrontendProperties frontendProperties;

    @Override
    public PairSessionCreateViewResponse create(PairSessionCreateRequest request, RequestScope scope) {
        rateLimitService.consume(RateLimitAction.PAIR_CREATE, scope);
        ValidatedPlanRequest creator = pairPreferencesValidationService.validate(request.getParticipantA(),
                request.getCity(), "participantA");
        PairSessionCreated created = pairSessionService.create(scope, creator);
        return PairSessionCreateViewResponse.builder()
                .sessionId(created.getSession().getPublicId())
                .inviteUrl(frontendProperties.baseUrl() + INVITE_PATH + created.getInviteToken())
                .participantToken(created.getParticipantToken())
                .expiresAt(created.getSession().getExpiresAt())
                .status(created.getSession().getStatus())
                .build();
    }

    @Override
    public PairJoinViewResponse join(PairJoinCreateRequest request, RequestScope scope) {
        PairJoinTarget target = pairSessionService.findJoinTarget(request.getInviteToken(), scope);
        ValidatedPlanRequest invitee = pairPreferencesValidationService.validate(request.getParticipantB(),
                target.getCity().getId(), "participantB");
        // Matching (catalog and optional AI) runs outside any transaction; join re-checks the session under a lock.
        PairMatch match = pairMatchingService.match(target.getCreatorPreferences(), invitee);
        PairSessionJoined joined = pairSessionService.join(target, scope, invitee, match);
        return PairJoinViewResponse.builder()
                .sessionId(joined.getSession().getPublicId())
                .participantToken(joined.getParticipantToken())
                .status(joined.getSession().getStatus())
                .build();
    }

    @Override
    public PairSessionViewResponse get(String sessionId, String participantToken) {
        return pairDetailsService.describe(pairAccessService.authenticate(sessionId, participantToken));
    }

    @Override
    public PairSessionViewResponse vote(String sessionId, String participantToken, PairVoteCreateRequest request) {
        PairAccess access = pairAccessService.authenticate(sessionId, participantToken);
        return pairDetailsService.describe(pairVotingService.vote(access, request.getEventId(), request.getVote()));
    }

    @Override
    public void revoke(String sessionId, String participantToken) {
        pairSessionService.revoke(pairAccessService.authenticate(sessionId, participantToken));
    }
}
