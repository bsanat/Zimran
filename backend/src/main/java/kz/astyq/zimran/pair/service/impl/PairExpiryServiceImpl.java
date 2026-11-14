package kz.astyq.zimran.pair.service.impl;

import kz.astyq.zimran.pair.model.entity.PairSession;
import kz.astyq.zimran.pair.model.enums.PairStatus;
import kz.astyq.zimran.pair.repository.PairSessionRepository;
import kz.astyq.zimran.pair.service.PairExpiryService;
import kz.astyq.zimran.pair.util.PairStatusPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PairExpiryServiceImpl implements PairExpiryService {

    private final PairSessionRepository pairSessionRepository;
    private final Clock clock;

    @Override
    @Transactional
    public boolean expireIfDue(PairSession session) {
        Objects.requireNonNull(session, "session");
        if (session.getStatus() == PairStatus.WAITING_FOR_B && !clock.instant().isBefore(session.getExpiresAt())) {
            PairStatusPolicy.transition(session, PairStatus.EXPIRED);
            pairSessionRepository.save(session);
        }
        return session.getStatus() == PairStatus.EXPIRED;
    }
}
