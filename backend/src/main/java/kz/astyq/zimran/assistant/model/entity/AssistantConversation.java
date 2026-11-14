package kz.astyq.zimran.assistant.model.entity;

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
 * One AI assistant chat of a scope: the assistant quota is counted when it starts, later results are turns.
 */
@Entity
@Table(name = "assistant_conversations")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SequenceGenerator(name = "seq", sequenceName = "s_assistant_conversations", allocationSize = 1)
@SQLDelete(sql = "update assistant_conversations set is_deleted = true, deleted_at = now() where id = ?")
@SQLRestriction("is_deleted = false")
public class AssistantConversation extends AuditableEntity {

    /** The id the client sends back (a random UUID). */
    @Column(name = "public_id", nullable = false, updatable = false, length = 36)
    private String publicId;

    @Column(name = "scope_id", nullable = false, updatable = false, length = 64)
    private String scopeId;

    /** Results given in this conversation. */
    @Column(name = "turns", nullable = false)
    private int turns;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;
}
