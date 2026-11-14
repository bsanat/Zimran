package kz.astyq.zimran.jsonsource.service;

import kz.astyq.zimran.event.model.domain.Event;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

/**
 * Turns the JSON feed into normalized {@link Event}s. The accepted record format is documented on the
 * implementation; a missing field becomes null, never a guessed value.
 */
public interface JsonEventNormalizer {

    /**
     * Events of a whole document: a top-level array of records, or an object with an {@code events} array.
     * Records that cannot be normalized are skipped. Any other document shape:
     * {@code ServiceValidationException(UPSTREAM_UNAVAILABLE)}.
     */
    List<Event> normalizeAll(JsonNode document);

    /**
     * One record, or empty when it lacks a title, a mappable category or a known status, or is not an object.
     */
    Optional<Event> normalize(JsonNode record);
}
