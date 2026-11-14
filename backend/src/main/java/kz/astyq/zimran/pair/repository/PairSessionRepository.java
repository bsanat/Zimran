package kz.astyq.zimran.pair.repository;

import jakarta.persistence.LockModeType;
import kz.astyq.zimran.pair.model.entity.PairSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PairSessionRepository extends JpaRepository<PairSession, Long> {

    Optional<PairSession> findByPublicId(String publicId);

    Optional<PairSession> findByInviteTokenHash(String inviteTokenHash);

    /**
     * The session, locked until the transaction ends, so status changes of one session are serialized.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PairSession s where s.id = :id")
    Optional<PairSession> findByIdForUpdate(@Param("id") Long id);
}
