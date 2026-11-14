package kz.astyq.zimran.ai.client.impl;

import kz.astyq.zimran.ai.client.GeminiClient;
import kz.astyq.zimran.ai.model.dto.ChatCompletionRequest;
import kz.astyq.zimran.ai.model.dto.ChatCompletionResponse;
import kz.astyq.zimran.ai.model.dto.ChatMessage;
import kz.astyq.zimran.ai.util.MessageCode;
import kz.astyq.zimran.core.configuration.GeminiProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class GeminiClientImpl implements GeminiClient {

    private final RestClient restClient;
    private final GeminiProperties properties;

    public GeminiClientImpl(@Qualifier("geminiRestClient") RestClient restClient, GeminiProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public String complete(ChatCompletionRequest request) {
        if (!properties.hasApiKey()) {
            throw unavailable(null);
        }
        ChatCompletionResponse response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);
        } catch (RestClientException e) {
            // RestClient messages hold the URL and status, never the Authorization header.
            throw unavailable(e);
        }
        List<ChatCompletionResponse.Choice> choices = response != null ? response.getChoices() : null;
        if (choices == null || choices.isEmpty() || choices.getFirst() == null) {
            throw unavailable(new IllegalStateException("No choices"));
        }
        ChatCompletionResponse.Choice choice = choices.getFirst();
        ChatMessage message = choice.getMessage();
        if ("length".equals(choice.getFinishReason())) {
            throw unavailable(new IllegalStateException("Answer truncated"));
        }
        if (message == null || message.getRefusal() != null || message.getContent() == null
                || message.getContent().isBlank()) {
            throw unavailable(new IllegalStateException("Empty or refused answer"));
        }
        return message.getContent();
    }

    private static ServiceValidationException unavailable(Throwable cause) {
        ServiceValidationException exception = new ServiceValidationException(ErrorCode.AI_UNAVAILABLE,
                MessageCode.AI_UNAVAILABLE);
        return cause != null ? exception.withCause(cause) : exception;
    }
}
