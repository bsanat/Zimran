package kz.astyq.zimran.guest.repository;

import kz.astyq.zimran.guest.model.entity.GuestScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface GuestScopeRepository extends JpaRepository<GuestScope, Long> {

    Optional<GuestScope> findByScopeId(String scopeId);

    /**
     * Sets {@code lastSeenAt} to {@code now} unless it was already set at or after {@code notBefore}.
     *
     * @return the number of rows updated (0 or 1)
     */
    @Modifying
    @Query("""
            update GuestScope g set g.lastSeenAt = :now, g.updatedAt = :now
            where g.scopeId = :scopeId and g.isDeleted = false
              and (g.lastSeenAt is null or g.lastSeenAt < :notBefore)
            """)
    int touchLastSeen(@Param("scopeId") String scopeId, @Param("now") Instant now,
                      @Param("notBefore") Instant notBefore);
}
