package kz.astyq.zimran.buddy.repository;

import kz.astyq.zimran.buddy.model.entity.BuddyRequest;
import kz.astyq.zimran.buddy.model.enums.BuddyRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BuddyRequestRepository extends JpaRepository<BuddyRequest, Long> {

    Optional<BuddyRequest> findByPublicId(String publicId);

    boolean existsByPostIdAndSenderScopeId(Long postId, String senderScopeId);

    List<BuddyRequest> findAllByPostIdAndStatus(Long postId, BuddyRequestStatus status);

    List<BuddyRequest> findAllByPostIdInOrderByCreatedAtAsc(Collection<Long> postIds);

    List<BuddyRequest> findTop50BySenderScopeIdOrderByCreatedAtDesc(String senderScopeId);

    /**
     * Requests in {@code statuses} that either scope sent to a post of the other.
     */
    @Query("""
            select r from BuddyRequest r, BuddyPost p
            where p.id = r.postId and r.status in :statuses
              and ((r.senderScopeId = :first and p.ownerScopeId = :second)
                or (r.senderScopeId = :second and p.ownerScopeId = :first))
            """)
    List<BuddyRequest> findBetween(@Param("first") String first, @Param("second") String second,
                                   @Param("statuses") Collection<BuddyRequestStatus> statuses);
}
