package kz.astyq.zimran.notification.repository;

import kz.astyq.zimran.notification.model.entity.Notification;
import kz.astyq.zimran.notification.model.enums.NotificationKind;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Inserts the notification and its {@code IN_APP} delivery ({@code DELIVERED}) in one statement, unless the
     * scope already has a notification of {@code kind} for the event. Atomic: concurrent calls for the same
     * (scope, event, kind) create exactly one row, with no read-then-write race. {@code titleArgs} is a JSON array
     * of strings. Call inside a transaction.
     *
     * @return 1 when created, 0 when it already existed
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            with inserted as (
                insert into notifications (id, public_id, scope_id, event_id, kind, title_key, title_args,
                                           category_slug, deliver_after, batch_key, created_at, updated_at, is_deleted)
                values (nextval('s_notifications'), :publicId, :scopeId, :eventId, :kind, :titleKey,
                        array(select jsonb_array_elements_text(cast(:titleArgs as jsonb))),
                        :categorySlug, :deliverAfter, cast(:batchKey as varchar), :now, :now, false)
                on conflict (scope_id, event_id, kind) where is_deleted = false do nothing
                returning id
            )
            insert into notification_deliveries (id, notification_id, channel, status, delivered_at,
                                                 created_at, updated_at, is_deleted)
            select nextval('s_notification_deliveries'), inserted.id, 'IN_APP', 'DELIVERED', :now, :now, :now, false
            from inserted
            """)
    int insertIfAbsent(@Param("publicId") String publicId,
                       @Param("scopeId") String scopeId,
                       @Param("eventId") Long eventId,
                       @Param("kind") String kind,
                       @Param("titleKey") String titleKey,
                       @Param("titleArgs") String titleArgs,
                       @Param("categorySlug") String categorySlug,
                       @Param("deliverAfter") Instant deliverAfter,
                       @Param("batchKey") String batchKey,
                       @Param("now") Instant now);

    /**
     * Catalog ids among {@code eventIds} the scope already has a notification of {@code kind} for.
     */
    @Query("select n.eventId from Notification n where n.scopeId = :scopeId and n.kind = :kind "
            + "and n.eventId in :eventIds")
    List<Long> findNotifiedEventIds(@Param("scopeId") String scopeId,
                                    @Param("kind") NotificationKind kind,
                                    @Param("eventIds") Collection<Long> eventIds);

    Slice<Notification> findByScopeIdAndDeliverAfterLessThanEqual(String scopeId, Instant now, Pageable pageable);

    long countByScopeIdAndDeliverAfterLessThanEqualAndReadAtIsNull(String scopeId, Instant now);

    Optional<Notification> findByPublicIdAndScopeIdAndDeliverAfterLessThanEqual(String publicId, String scopeId,
                                                                                 Instant now);

    /**
     * Marks every delivered, unread notification of the scope as read. Call inside a transaction.
     *
     * @return the number of notifications changed
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            update notifications set read_at = :now, updated_at = :now
            where scope_id = :scopeId and read_at is null and deliver_after <= :now and is_deleted = false
            """)
    int markAllRead(@Param("scopeId") String scopeId, @Param("now") Instant now);
}
