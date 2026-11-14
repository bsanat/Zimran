package kz.astyq.zimran.assistant.service;

import kz.astyq.zimran.assistant.model.domain.AssistantFilters;

/**
 * Reads search filters out of a guest's free-text message.
 */
public interface AssistantParseService {

    /**
     * Lets the AI extract the filters (relative dates resolved against today in the city the guest is looking at),
     * then validates every field on the server ({@link AssistantFilterResolutionService#resolve}). AI unavailable:
     * {@code ServiceValidationException(AI_UNAVAILABLE)}.
     *
     * @param requestedCity city the guest is looking at (id or name), used when the message names none; may be null
     */
    AssistantFilters parse(String message, String requestedCity);
}
