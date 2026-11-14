package kz.astyq.zimran.pair.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import kz.astyq.zimran.core.model.AuditableEntity;
import kz.astyq.zimran.pair.model.enums.PairRole;
import kz.astyq.zimran.pair.model.enums.PairVoteType;
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
 * A side's vote on one suggested event (public event id).
 */
@Entity
@Table(name = "pair_votes")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SequenceGenerator(name = "seq", sequenceName = "s_pair_votes", allocationSize = 1)
@SQLDelete(sql = "update pair_votes set is_deleted = true, deleted_at = now() where id = ?")
@SQLRestriction("is_deleted = false")
public class PairVote extends AuditableEntity {

    @Column(name = "session_id", nullable = false, updatable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false, updatable = false, length = 1)
    private PairRole role;

    @Column(name = "event_id", nullable = false, updatable = false, length = 300)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote", nullable = false, length = 8)
    private PairVoteType vote;
}
