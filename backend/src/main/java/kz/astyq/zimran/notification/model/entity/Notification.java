package kz.astyq.zimran.notification.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import kz.astyq.zimran.core.model.AuditableEntity;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import kz.astyq.zimran.notification.model.enums.NotificationKind;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

/**
 * One in-app notification. Rows are created only by {@code NotificationRepository#insertIfAbsent}; the title is
 * rendered on read from {@code titleKey} + {@code titleArgs}.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SequenceGenerator(name = "seq", sequenceName = "s_notifications", allocationSize = 1)
@SQLDelete(sql = "update notifications set is_deleted = true, deleted_at = now() where id = ?")
@SQLRestriction("is_deleted = false")
public class Notification extends AuditableEntity {

    @Column(name = "public_id", nullable = false, updatable = false, length = 64)
    private String publicId;

    @Column(name = "scope_id", nullable = false, updatable = false, length = 64)
    private String scopeId;

    /** Internal catalog id of the event. */
    @Column(name = "event_id", nullable = false, updatable = false)
    private Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, updatable = false, length = 32)
    private NotificationKind kind;

    @Column(name = "title_key", nullable = false, updatable = false, length = 100)
    private String titleKey;

    /** Message arguments of {@link #titleKey}; untrusted text (e.g. the event title). */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "title_args", nullable = false, updatable = false, columnDefinition = "text[]")
    private List<String> titleArgs;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_slug", nullable = false, updatable = false, length = 32)
    private CategorySlug categorySlug;

    /** Hidden from the notification center until then. */
    @Column(name = "deliver_after", nullable = false)
    private Instant deliverAfter;

    @Column(name = "read_at")
    private Instant readAt;

    /** DAILY / WEEKLY delivery window, e.g. {@code daily:2026-11-15}; null for INSTANT. */
    @Column(name = "batch_key", length = 64)
    private String batchKey;
}
