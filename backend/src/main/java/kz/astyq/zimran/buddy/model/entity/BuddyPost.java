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
import kz.astyq.zimran.buddy.model.enums.BuddyPostStatus;

import java.time.Instant;

/**
 * An Event Buddy post. {@code ownerScopeId} never leaves the backend; no contact data is stored.
 */
@Entity
@Table(name = "buddy_posts")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SequenceGenerator(name = "seq", sequenceName = "s_buddy_posts", allocationSize = 1)
@SQLDelete(sql = "update buddy_posts set is_deleted = true, deleted_at = now() where id = ?")
@SQLRestriction("is_deleted = false")
public class BuddyPost extends AuditableEntity {

    @Column(name = "public_id", nullable = false, updatable = false, length = 64)
    private String publicId;

    /** Public event id ({@code json:...}). */
    @Column(name = "event_id", nullable = false, updatable = false, length = 300)
    private String eventId;

    @Column(name = "owner_scope_id", nullable = false, updatable = false, length = 64)
    private String ownerScopeId;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "short_bio", nullable = false, length = 300)
    private String shortBio;

    @Column(name = "language", length = 32)
    private String language;

    @Column(name = "preferences", length = 300)
    private String preferences;

    @Column(name = "age_min")
    private Integer ageMin;

    @Column(name = "age_max")
    private Integer ageMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private BuddyPostStatus status;

    /** Start of the event's next occurrence when the post was made. */
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;
}
