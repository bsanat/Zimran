package kz.astyq.zimran.buddy.repository;

import jakarta.persistence.LockModeType;
import kz.astyq.zimran.buddy.model.entity.BuddyPost;
import kz.astyq.zimran.buddy.model.enums.BuddyPostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BuddyPostRepository extends JpaRepository<BuddyPost, Long> {

    Optional<BuddyPost> findByPublicId(String publicId);

    /**
     * The post, locked until the transaction ends, so status changes of one post (and its requests) are serialized.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from BuddyPost p where p.id = :id")
    Optional<BuddyPost> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select p from BuddyPost p
            where p.eventId = :eventId and p.status = :status and p.expiresAt > :now
            order by p.createdAt desc
            """)
    List<BuddyPost> findByEventAndStatusNotExpired(@Param("eventId") String eventId,
                                                   @Param("status") BuddyPostStatus status,
                                                   @Param("now") Instant now);

    boolean existsByOwnerScopeIdAndEventIdAndStatus(String ownerScopeId, String eventId, BuddyPostStatus status);

    List<BuddyPost> findTop50ByOwnerScopeIdOrderByCreatedAtDesc(String ownerScopeId);

    List<BuddyPost> findAllByIdIn(Collection<Long> ids);

    /**
     * Sets {@code expired} on every post in {@code active} whose event has started.
     */
    @Modifying
    @Query("""
            update BuddyPost p set p.status = :expired, p.updatedAt = :now
            where p.status in :active and p.expiresAt <= :now
            """)
    int expireDue(@Param("expired") BuddyPostStatus expired, @Param("active") Collection<BuddyPostStatus> active,
                  @Param("now") Instant now);
}
