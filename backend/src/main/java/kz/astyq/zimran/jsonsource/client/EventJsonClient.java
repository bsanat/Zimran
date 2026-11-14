package kz.astyq.zimran.jsonsource.client;

import tools.jackson.databind.JsonNode;

/**
 * Reads the raw JSON event document from {@code app.events-source.location} (a file or an http(s) URL).
 */
public interface EventJsonClient {

    boolean isConfigured();

    /**
     * Cheap identity of the current document version (file: size and modification time), or null when the
     * source cannot tell without downloading it (URL) or the file is missing.
     */
    String fingerprint();

    /**
     * The parsed document. Not configured, unreadable, too large or invalid JSON:
     * {@code ServiceValidationException(UPSTREAM_UNAVAILABLE)}.
     */
    JsonNode fetch();
}
