package kz.astyq.zimran.pair.repository;

import kz.astyq.zimran.pair.model.entity.PairSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PairSuggestionRepository extends JpaRepository<PairSuggestion, Long> {

    List<PairSuggestion> findBySessionIdOrderByRankAsc(Long sessionId);
}
