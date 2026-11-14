package kz.astyq.zimran.subscription.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import kz.astyq.zimran.core.model.AuditableEntity;
import kz.astyq.zimran.subscription.model.enums.SubscriptionStatus;
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
 * Entitlement of one scope (guest or user). No row = {@link SubscriptionStatus#FREE}.
 */
@Entity
@Table(name = "subscription_entitlements")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SequenceGenerator(name = "seq", sequenceName = "s_subscription_entitlements", allocationSize = 1)
@SQLDelete(sql = "update subscription_entitlements set is_deleted = true, deleted_at = now() where id = ?")
@SQLRestriction("is_deleted = false")
public class SubscriptionEntitlement extends AuditableEntity {

    @Column(name = "scope_id", nullable = false, updatable = false, length = 64)
    private String scopeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SubscriptionStatus status;

    @Column(name = "provider", length = 32)
    private String provider;

    @Column(name = "provider_subscription_id")
    private String providerSubscriptionId;

    @Column(name = "period_end")
    private Instant periodEnd;

    @Column(name = "demo_only", nullable = false)
    private boolean demoOnly;
}
