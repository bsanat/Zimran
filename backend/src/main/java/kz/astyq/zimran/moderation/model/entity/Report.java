package kz.astyq.zimran.moderation.model.entity;

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
import kz.astyq.zimran.moderation.model.enums.ReportReason;
import kz.astyq.zimran.moderation.model.enums.ReportTargetType;

/**
 * A user report for moderation. {@code reportedScopeId} is resolved on the server and never returned.
 */
@Entity
@Table(name = "reports")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SequenceGenerator(name = "seq", sequenceName = "s_reports", allocationSize = 1)
@SQLDelete(sql = "update reports set is_deleted = true, deleted_at = now() where id = ?")
@SQLRestriction("is_deleted = false")
public class Report extends AuditableEntity {

    @Column(name = "reporter_scope_id", nullable = false, updatable = false, length = 64)
    private String reporterScopeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, updatable = false, length = 16)
    private ReportTargetType targetType;

    /** The public id the reporter named. */
    @Column(name = "target_id", nullable = false, updatable = false, length = 64)
    private String targetId;

    @Column(name = "reported_scope_id", nullable = false, updatable = false, length = 64)
    private String reportedScopeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, updatable = false, length = 32)
    private ReportReason reason;

    @Column(name = "comment", length = 500)
    private String comment;
}
