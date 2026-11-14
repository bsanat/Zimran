package kz.astyq.zimran.moderation.repository;

import kz.astyq.zimran.moderation.model.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {

    Optional<Block> findByPublicId(String publicId);

    Optional<Block> findByBlockerScopeIdAndBlockedScopeId(String blockerScopeId, String blockedScopeId);

    @Query("""
            select case when count(b) > 0 then true else false end from Block b
            where (b.blockerScopeId = :first and b.blockedScopeId = :second)
               or (b.blockerScopeId = :second and b.blockedScopeId = :first)
            """)
    boolean existsEitherWay(@Param("first") String first, @Param("second") String second);

    /** Scopes {@code scopeId} blocked, and scopes that blocked {@code scopeId}. */
    @Query("""
            select case when b.blockerScopeId = :scopeId then b.blockedScopeId else b.blockerScopeId end
            from Block b where b.blockerScopeId = :scopeId or b.blockedScopeId = :scopeId
            """)
    List<String> findCounterpartsOf(@Param("scopeId") String scopeId);
}
