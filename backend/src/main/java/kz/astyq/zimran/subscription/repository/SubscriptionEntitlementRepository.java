package kz.astyq.zimran.subscription.repository;

import kz.astyq.zimran.subscription.model.entity.SubscriptionEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface SubscriptionEntitlementRepository extends JpaRepository<SubscriptionEntitlement, Long> {

    Optional<SubscriptionEntitlement> findByScopeId(String scopeId);

    /**
     * Atomically gives the scope {@code DEMO_PREMIUM} (demo only), creating the row if needed. An active paid
     * subscription is left untouched. Safe to call repeatedly and concurrently.
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            insert into subscription_entitlements (id, scope_id, status, demo_only, created_at, updated_at, is_deleted)
            values (nextval('s_subscription_entitlements'), :scopeId, 'DEMO_PREMIUM', true, :now, :now, false)
            on conflict (scope_id) where is_deleted = false
            do update set status = 'DEMO_PREMIUM', demo_only = true, updated_at = :now
            where subscription_entitlements.status = 'FREE'
            """)
    int upsertDemoPremium(@Param("scopeId") String scopeId, @Param("now") Instant now);

    /**
     * Atomically returns a {@code DEMO_PREMIUM} scope to {@code FREE}. Any other status (a paid subscription, or
     * no row at all) is left untouched. Safe to call repeatedly and concurrently.
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            update subscription_entitlements set status = 'FREE', demo_only = false, updated_at = :now
            where scope_id = :scopeId and is_deleted = false and status = 'DEMO_PREMIUM'
            """)
    int downgradeDemoPremium(@Param("scopeId") String scopeId, @Param("now") Instant now);
}
