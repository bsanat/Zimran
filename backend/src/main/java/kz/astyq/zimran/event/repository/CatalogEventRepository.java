package kz.astyq.zimran.event.repository;

import kz.astyq.zimran.event.model.entity.CatalogEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CatalogEventRepository extends JpaRepository<CatalogEvent, Long>, JpaSpecificationExecutor<CatalogEvent> {

    Optional<CatalogEvent> findByProviderAndProviderEventId(String provider, String providerEventId);

    List<CatalogEvent> findByProviderAndProviderEventIdIn(String provider, Collection<String> providerEventIds);

    List<CatalogEvent> findByDemoTrueAndFirstSeenAtGreaterThanEqualOrderByFirstSeenAtAscIdAsc(Instant since);

    /**
     * Takes the transaction-scoped advisory lock {@code key}, waiting while another transaction holds it; it is
     * released at commit or rollback. Serializes catalog upserts across threads and instances. Call inside a
     * transaction.
     */
    @Query(nativeQuery = true, value = "select 1 from pg_advisory_xact_lock(:key)")
    Integer lockForUpdate(@Param("key") long key);

    /**
     * Marks events {@code EXPIRED} whose last day (in their own timezone) is before the day of {@code now} there.
     * Open-ended and undated events never expire. Call inside a transaction.
     *
     * @return the number of rows changed
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            update events set status = 'EXPIRED', updated_at = :now
            where is_deleted = false
              and status in ('ACTIVE', 'POSTPONED')
              and ends_on is not null
              and ends_on < cast((cast(:now as timestamptz) at time zone events.timezone) as date)
            """)
    int expireEndedBefore(@Param("now") Instant now);
}
