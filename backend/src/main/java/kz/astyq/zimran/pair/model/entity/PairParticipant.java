package kz.astyq.zimran.pair.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import kz.astyq.zimran.core.model.AuditableEntity;
import kz.astyq.zimran.pair.model.enums.PairRole;
import kz.astyq.zimran.planner.model.enums.TimePreference;
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
import java.time.LocalDate;
import java.util.List;

/**
 * One side of a session with its validated inputs. {@code message} is sanitized but untrusted text and is never
 * shown to the other side.
 */
@Entity
@Table(name = "pair_participants")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SequenceGenerator(name = "seq", sequenceName = "s_pair_participants", allocationSize = 1)
@SQLDelete(sql = "update pair_participants set is_deleted = true, deleted_at = now() where id = ?")
@SQLRestriction("is_deleted = false")
public class PairParticipant extends AuditableEntity {

    @Column(name = "session_id", nullable = false, updatable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false, updatable = false, length = 1)
    private PairRole role;

    @Column(name = "scope_id", nullable = false, updatable = false, length = 64)
    private String scopeId;

    /** SHA-256 (hex) of the participant token. */
    @Column(name = "access_token_hash", nullable = false, updatable = false, length = 64)
    private String accessTokenHash;

    /** Interest ids. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "interests", nullable = false, columnDefinition = "text[]")
    private List<String> interests;

    /** Null = no budget limit. */
    @Column(name = "budget_max")
    private BigDecimal budgetMax;

    @Column(name = "date_from", nullable = false)
    private LocalDate dateFrom;

    @Column(name = "date_to", nullable = false)
    private LocalDate dateTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_preference", length = 16)
    private TimePreference timePreference;

    @Column(name = "message")
    private String message;
}
