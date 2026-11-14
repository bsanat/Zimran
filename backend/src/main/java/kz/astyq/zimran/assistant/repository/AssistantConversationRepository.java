package kz.astyq.zimran.assistant.repository;

import kz.astyq.zimran.assistant.model.entity.AssistantConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AssistantConversationRepository extends JpaRepository<AssistantConversation, Long> {

    Optional<AssistantConversation> findByPublicId(String publicId);

    /**
     * Atomically counts one more turn while the conversation is open and under {@code maxTurns}; 0 when it is not
     * (expired or at the limit, possibly by a concurrent request).
     */
    @Modifying
    @Query("""
            update AssistantConversation c set c.turns = c.turns + 1, c.updatedAt = :now
            where c.id = :id and c.turns < :maxTurns and c.expiresAt > :now
            """)
    int incrementTurns(@Param("id") Long id, @Param("maxTurns") int maxTurns, @Param("now") Instant now);
}
