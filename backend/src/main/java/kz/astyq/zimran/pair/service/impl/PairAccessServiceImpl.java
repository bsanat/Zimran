package kz.astyq.zimran.pair.service.impl;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.pair.model.domain.PairAccess;
import kz.astyq.zimran.pair.model.entity.PairParticipant;
import kz.astyq.zimran.pair.model.entity.PairSession;
import kz.astyq.zimran.pair.repository.PairParticipantRepository;
import kz.astyq.zimran.pair.repository.PairSessionRepository;
import kz.astyq.zimran.pair.service.PairAccessService;
import kz.astyq.zimran.pair.service.PairExpiryService;
import kz.astyq.zimran.pair.service.PairTokenService;
import kz.astyq.zimran.pair.util.MessageCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PairAccessServiceImpl implements PairAccessService {

    private static final int MAX_SESSION_ID_LENGTH = 64;

    private final PairSessionRepository pairSessionRepository;
    private final PairParticipantRepository pairParticipantRepository;
    private final PairTokenService pairTokenService;
    private final PairExpiryService pairExpiryService;

    @Override
    @Transactional
    public PairAccess authenticate(String sessionId, String participantToken) {
        if (!StringUtils.hasText(sessionId) || sessionId.length() > MAX_SESSION_ID_LENGTH
                || !StringUtils.hasText(participantToken)) {
            throw notFound();
        }
        PairSession session = pairSessionRepository.findByPublicId(sessionId).orElseThrow(PairAccessServiceImpl::notFound);
        List<PairParticipant> participants = pairParticipantRepository.findBySessionIdOrderByRoleAsc(session.getId());
        PairParticipant me = null;
        // Every hash is compared, so the timing does not depend on which side the token belongs to.
        for (PairParticipant participant : participants) {
            if (pairTokenService.matches(participantToken, participant.getAccessTokenHash()) && me == null) {
                me = participant;
            }
        }
        if (me == null) {
            throw notFound();
        }
        PairParticipant self = me;
        PairParticipant partner = participants.stream()
                .filter(participant -> participant.getRole() == self.getRole().other())
                .findFirst()
                .orElse(null);
        pairExpiryService.expireIfDue(session);
        return PairAccess.builder().session(session).me(me).partner(partner).build();
    }

    private static ServiceValidationException notFound() {
        return new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.PAIR_NOT_FOUND);
    }
}
