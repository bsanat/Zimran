package kz.astyq.zimran.alert.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import kz.astyq.zimran.alert.model.enums.AlertFrequency;
import kz.astyq.zimran.core.model.AuditableEntity;
import kz.astyq.zimran.core.model.enums.ScopeKind;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Alert preferences of one scope. {@code categorySlugs} and {@code channels} hold enum names.
 */
@Entity
@Table(name = "alert_preferences")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SequenceGenerator(name = "seq", sequenceName = "s_alert_preferences", allocationSize = 1)
@SQLDelete(sql = "update alert_preferences set is_deleted = true, deleted_at = now() where id = ?")
@SQLRestriction("is_deleted = false")
public class AlertPreference extends AuditableEntity {

    @Column(name = "scope_id", nullable = false, updatable = false, length = 64)
    private String scopeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_kind", nullable = false, updatable = false, length = 16)
    private ScopeKind scopeKind;

    /** Supported city id. */
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "category_slugs", nullable = false, columnDefinition = "text[]")
    private List<String> categorySlugs;

    @Column(name = "budget_max")
    private BigDecimal budgetMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 16)
    private AlertFrequency frequency;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "channels", nullable = false, columnDefinition = "text[]")
    private List<String> channels;

    @Column(name = "paused", nullable = false)
    private boolean paused;

    /** When alerts were first enabled or last un-paused; null while never enabled. */
    @Column(name = "enabled_at")
    private Instant enabledAt;
}
