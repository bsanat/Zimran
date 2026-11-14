package kz.astyq.zimran.pair.repository;

import kz.astyq.zimran.pair.model.entity.PairParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PairParticipantRepository extends JpaRepository<PairParticipant, Long> {

    List<PairParticipant> findBySessionIdOrderByRoleAsc(Long sessionId);
}
