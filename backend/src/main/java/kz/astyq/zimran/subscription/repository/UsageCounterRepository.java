package kz.astyq.zimran.subscription.repository;

import kz.astyq.zimran.subscription.model.entity.UsageCounter;
import kz.astyq.zimran.subscription.model.enums.UsageFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface UsageCounterRepository extends JpaRepository<UsageCounter, Long> {

    @Query("""
            select c.usageCount from UsageCounter c
            where c.scopeId = :scopeId and c.feature = :feature and c.windowStart = :windowStart
            """)
    Optional<Integer> findUsageCount(@Param("scopeId") String scopeId, @Param("feature") UsageFeature feature,
                                     @Param("windowStart") Instant windowStart);

    /**
     * Atomically counts one use in the window if the count is below {@code limit} (which must be at least 1).
     *
     * @param feature the {@link UsageFeature} name
     * @return the new count, or empty when the limit was already reached
     */
    @Transactional
    @Query(nativeQuery = true, value = """
            insert into usage_counters (id, scope_id, feature, window_start, count, created_at, updated_at, is_deleted)
            values (nextval('s_usage_counters'), :scopeId, :feature, :windowStart, 1, :now, :now, false)
            on conflict (scope_id, feature, window_start) where is_deleted = false
            do update set count = usage_counters.count + 1, updated_at = :now
            where usage_counters.count < :limit
            returning count
            """)
    Optional<Integer> incrementIfBelow(@Param("scopeId") String scopeId, @Param("feature") String feature,
                                       @Param("windowStart") Instant windowStart, @Param("limit") int limit,
                                       @Param("now") Instant now);
}
