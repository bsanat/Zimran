package kz.astyq.zimran.ai.service;

import kz.astyq.zimran.ai.model.domain.AiExtraction;
import kz.astyq.zimran.ai.model.domain.AiExtractionRequest;

/**
 * Asks the model to read search filters out of a guest's free-text message. The model only extracts; it never
 * decides what is valid.
 */
public interface AiFilterExtractionService {

    /**
     * The sanitized, unverified filters. AI unavailable (no API key, usage cap, error, timeout) or an answer that
     * breaks the schema: {@code ServiceValidationException(AI_UNAVAILABLE)}, never a guessed result.
     */
    AiExtraction extract(AiExtractionRequest request);
}
