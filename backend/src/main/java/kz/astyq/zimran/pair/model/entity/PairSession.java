package kz.astyq.zimran.pair.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import kz.astyq.zimran.core.model.AuditableEntity;
import kz.astyq.zimran.pair.model.enums.PairConflictType;
import kz.astyq.zimran.pair.model.enums.PairStatus;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

/**
 * A Pair Planning session. Change {@code status} only through {@code PairStatusPolicy}.
 */
@Entity
@Table(name = "pair_sessions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SequenceGenerator(name = "seq", sequenceName = "s_pair_sessions", allocationSize = 1)
@SQLDelete(sql = "update pair_sessions set is_deleted = true, deleted_at = now() where id = ?")
@SQLRestriction("is_deleted = false")
public class PairSession extends AuditableEntity {

    @Column(name = "public_id", nullable = false, updatable = false, length = 64)
    private String publicId;

    @Column(name = "creator_scope_id", nullable = false, updatable = false, length = 64)
    private String creatorScopeId;

    /** SHA-256 (hex) of the invite token; the token itself is never stored. */
    @Column(name = "invite_token_hash", nullable = false, updatable = false, length = 64)
    private String inviteTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PairStatus status;

    /** Supported city id. */
    @Column(name = "city", nullable = false, updatable = false, length = 100)
    private String city;

    /** End of the invite; after it an unjoined session is {@code EXPIRED}. */
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    /** Public event id liked by both sides ({@code MATCHED}). */
    @Column(name = "matched_event_id", length = 300)
    private String matchedEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "conflict_type", length = 16)
    private PairConflictType conflictType;

    /** Constraint the sides could relax ({@code NO_MATCH}). */
    @Enumerated(EnumType.STRING)
    @Column(name = "conflict_relax", length = 16)
    private PairConflictType conflictRelax;
}
