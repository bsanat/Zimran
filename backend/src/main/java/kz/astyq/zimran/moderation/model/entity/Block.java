package kz.astyq.zimran.moderation.model.entity;

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

/**
 * One scope blocking another. {@code publicId} is the scopeRef the blocker uses to lift it.
 */
@Entity
@Table(name = "blocks")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SequenceGenerator(name = "seq", sequenceName = "s_blocks", allocationSize = 1)
@SQLDelete(sql = "update blocks set is_deleted = true, deleted_at = now() where id = ?")
@SQLRestriction("is_deleted = false")
public class Block extends AuditableEntity {

    @Column(name = "public_id", nullable = false, updatable = false, length = 64)
    private String publicId;

    @Column(name = "blocker_scope_id", nullable = false, updatable = false, length = 64)
    private String blockerScopeId;

    @Column(name = "blocked_scope_id", nullable = false, updatable = false, length = 64)
    private String blockedScopeId;
}
