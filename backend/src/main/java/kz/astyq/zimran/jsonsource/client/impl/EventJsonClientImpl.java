package kz.astyq.zimran.jsonsource.client.impl;

import kz.astyq.zimran.core.configuration.EventSourceProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.jsonsource.client.EventJsonClient;
import kz.astyq.zimran.jsonsource.util.MessageCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * File or URL reader for the JSON feed. The location is never logged: a URL may carry a token.
 */
@Slf4j
@Component
public class EventJsonClientImpl implements EventJsonClient {

    private final EventSourceProperties properties;
    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public EventJsonClientImpl(EventSourceProperties properties,
                               @Qualifier("eventSourceRestClient") RestClient restClient,
                               JsonMapper jsonMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public String fingerprint() {
        if (!properties.isConfigured() || properties.isUrl()) {
            return null;
        }
        try {
            Path path = path();
            return Files.size(path) + "@" + Files.getLastModifiedTime(path).toMillis();
        } catch (IOException | InvalidPathException e) {
            return null;
        }
    }

    @Override
    public JsonNode fetch() {
        if (!properties.isConfigured()) {
            throw new ServiceValidationException(ErrorCode.UPSTREAM_UNAVAILABLE, MessageCode.EVENTS_SOURCE_NOT_CONFIGURED);
        }
        byte[] body = properties.isUrl() ? download() : readFile();
        if (body == null || body.length == 0) {
            throw unavailable(null);
        }
        if (body.length > properties.getMaxBytes()) {
            throw unavailable(new IOException("Event document exceeds " + properties.getMaxBytes() + " bytes"));
        }
        try {
            return jsonMapper.readTree(body);
        } catch (JacksonException e) {
            throw unavailable(e);
        }
    }

    private byte[] readFile() {
        try {
            Path path = path();
            if (Files.size(path) > properties.getMaxBytes()) {
                throw unavailable(new IOException("Event document exceeds " + properties.getMaxBytes() + " bytes"));
            }
            return Files.readAllBytes(path);
        } catch (IOException | InvalidPathException e) {
            throw unavailable(e);
        }
    }

    private byte[] download() {
        try {
            return restClient.get().uri(properties.getLocation().strip()).retrieve().body(byte[].class);
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    private Path path() {
        return Path.of(properties.getLocation().strip());
    }

    private static ServiceValidationException unavailable(Throwable cause) {
        ServiceValidationException exception = new ServiceValidationException(ErrorCode.UPSTREAM_UNAVAILABLE,
                MessageCode.EVENTS_SOURCE_UNAVAILABLE);
        return cause != null ? exception.withCause(cause) : exception;
    }
}
