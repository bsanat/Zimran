package kz.astyq.zimran.subscription.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import kz.astyq.zimran.core.model.AuditableEntity;
import kz.astyq.zimran.subscription.model.enums.UsageFeature;
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
 * Uses of a feature by one scope within one quota window.
 */
@Entity
@Table(name = "usage_counters")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SequenceGenerator(name = "seq", sequenceName = "s_usage_counters", allocationSize = 1)
@SQLDelete(sql = "update usage_counters set is_deleted = true, deleted_at = now() where id = ?")
@SQLRestriction("is_deleted = false")
public class UsageCounter extends AuditableEntity {

    @Column(name = "scope_id", nullable = false, updatable = false, length = 64)
    private String scopeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature", nullable = false, updatable = false, length = 32)
    private UsageFeature feature;

    @Column(name = "window_start", nullable = false, updatable = false)
    private Instant windowStart;

    @Column(name = "count", nullable = false)
    private int usageCount;
}
