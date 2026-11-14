package kz.astyq.zimran.pair.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import kz.astyq.zimran.core.model.AuditableEntity;
import kz.astyq.zimran.pair.model.enums.PairRole;
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

/**
 * An event suggested to both sides (computed once when B joins). The event data itself is always read from the
 * catalog by {@code eventId}; only the reasons are stored.
 */
@Entity
@Table(name = "pair_suggestions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SequenceGenerator(name = "seq", sequenceName = "s_pair_suggestions", allocationSize = 1)
@SQLDelete(sql = "update pair_suggestions set is_deleted = true, deleted_at = now() where id = ?")
@SQLRestriction("is_deleted = false")
public class PairSuggestion extends AuditableEntity {

    @Column(name = "session_id", nullable = false, updatable = false)
    private Long sessionId;

    @Column(name = "event_id", nullable = false, updatable = false, length = 300)
    private String eventId;

    /** 1 = best. */
    @Column(name = "rank", nullable = false, updatable = false)
    private int rank;

    @Column(name = "reason_a", nullable = false)
    private String reasonA;

    @Column(name = "reason_b", nullable = false)
    private String reasonB;

    /** Side whose interest the event serves when only one side's interests match; null = a shared interest. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "compromise", length = 1)
    private PairRole compromise;
}
