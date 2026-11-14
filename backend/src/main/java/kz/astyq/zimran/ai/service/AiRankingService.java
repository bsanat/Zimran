package kz.astyq.zimran.ai.service;

import kz.astyq.zimran.ai.model.domain.AiRanking;
import kz.astyq.zimran.ai.model.domain.AiRankingRequest;

import java.util.Optional;

/**
 * Asks the model to rank candidate events and explain its picks.
 */
public interface AiRankingService {

    /**
     * The sanitized, schema-valid answer, or empty when AI cannot answer: no API key, usage cap reached, no
     * candidates, error, {@code app.openai.total-timeout} exceeded or an answer that breaks the schema. Never
     * throws for those cases, so the caller can always fall back. Picks are not yet checked against the
     * candidates.
     */
    Optional<AiRanking> rank(AiRankingRequest request);
}
