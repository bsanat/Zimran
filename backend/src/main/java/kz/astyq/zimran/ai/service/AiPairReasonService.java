package kz.astyq.zimran.ai.service;

import kz.astyq.zimran.ai.model.domain.AiPairReason;
import kz.astyq.zimran.ai.model.domain.AiPairReasonRequest;

import java.util.List;
import java.util.Optional;

/**
 * Asks the model for a reason per side for each event of a Pair Planning match.
 */
public interface AiPairReasonService {

    /**
     * The sanitized reasons, or empty when the AI is unavailable or its answer does not match the schema. Never
     * throws for AI failures: matching must not depend on the AI.
     */
    Optional<List<AiPairReason>> explain(AiPairReasonRequest request);
}
