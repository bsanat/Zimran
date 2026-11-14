package kz.astyq.zimran.guest.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.Instant;

/**
 * An anonymous guest. The guest token proves the {@code scopeId}; this row holds what we know about the guest.
 */
@Entity
@Table(name = "guest_scopes")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SequenceGenerator(name = "seq", sequenceName = "s_guest_scopes", allocationSize = 1)
@SQLDelete(sql = "update guest_scopes set is_deleted = true, deleted_at = now() where id = ?")
@SQLRestriction("is_deleted = false")
public class GuestScope extends AuditableEntity {

    @Column(name = "scope_id", nullable = false, updatable = false, length = 64)
    private String scopeId;

    @Column(name = "age_confirmed", nullable = false)
    private boolean ageConfirmed;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
}
