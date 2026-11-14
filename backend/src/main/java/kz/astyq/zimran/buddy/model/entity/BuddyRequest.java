package kz.astyq.zimran.buddy.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import kz.astyq.zimran.core.model.AuditableEntity;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import kz.astyq.zimran.buddy.model.enums.BuddyRequestStatus;

import java.time.Instant;

/**
 * A request to join a Buddy post. {@code senderScopeId} never leaves the backend.
 */
@Entity
@Table(name = "buddy_requests")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SequenceGenerator(name = "seq", sequenceName = "s_buddy_requests", allocationSize = 1)
@SQLDelete(sql = "update buddy_requests set is_deleted = true, deleted_at = now() where id = ?")
@SQLRestriction("is_deleted = false")
public class BuddyRequest extends AuditableEntity {

    @Column(name = "public_id", nullable = false, updatable = false, length = 64)
    private String publicId;

    /** Internal id of the {@link BuddyPost}. */
    @Column(name = "post_id", nullable = false, updatable = false)
    private Long postId;

    @Column(name = "sender_scope_id", nullable = false, updatable = false, length = 64)
    private String senderScopeId;

    @Column(name = "sender_display_name", nullable = false, length = 50)
    private String senderDisplayName;

    @Column(name = "message", nullable = false, length = 300)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private BuddyRequestStatus status;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
