package kz.astyq.zimran.pair.repository;

import kz.astyq.zimran.pair.model.entity.PairVote;
import kz.astyq.zimran.pair.model.enums.PairRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PairVoteRepository extends JpaRepository<PairVote, Long> {

    List<PairVote> findBySessionId(Long sessionId);

    Optional<PairVote> findBySessionIdAndRoleAndEventId(Long sessionId, PairRole role, String eventId);
}
