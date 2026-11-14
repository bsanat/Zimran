package kz.astyq.zimran.ai.client.impl;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import kz.astyq.zimran.ai.model.dto.ChatCompletionRequest;
import kz.astyq.zimran.ai.model.dto.ChatMessage;
import kz.astyq.zimran.core.configuration.HttpClientConfig;
import kz.astyq.zimran.core.configuration.OpenAiProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiClientImplTest {

    private WireMockServer wireMock;
    private OpenAiProperties properties;
    private OpenAiClientImpl client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        properties = new OpenAiProperties();
        properties.setApiKey("sk-test");
        RestClient restClient = HttpClientConfig.restClient(RestClient.builder(), Duration.ofSeconds(1),
                Duration.ofMillis(500), wireMock.baseUrl());
        client = new OpenAiClientImpl(restClient, properties);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        @DisplayName("posts the request with the bearer key and returns the first choice content")
        void ok() {
            wireMock.stubFor(post(urlEqualTo("/chat/completions")).willReturn(json(
                    "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"{}\"},\"finish_reason\":\"stop\"}]}")));

            assertThat(client.complete(request())).isEqualTo("{}");
            wireMock.verify(postRequestedFor(urlEqualTo("/chat/completions"))
                    .withHeader("Authorization", equalTo("Bearer sk-test"))
                    .withRequestBody(equalToJson("{\"model\":\"m\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],"
                            + "\"response_format\":{\"type\":\"json_object\"},\"max_completion_tokens\":10}")));
        }

        @Test
        @DisplayName("HTTP error, refusal, truncation, empty choices or timeout: AI_UNAVAILABLE")
        void failures() {
            String[] bodies = {
                    "{\"choices\":[{\"message\":{\"refusal\":\"no\"}}]}",
                    "{\"choices\":[{\"message\":{\"content\":\"{\\\"a\\\"\"},\"finish_reason\":\"length\"}]}",
                    "{\"choices\":[]}"
            };
            for (String body : bodies) {
                wireMock.stubFor(post(urlEqualTo("/chat/completions")).willReturn(json(body)));
                assertUnavailable();
            }
            wireMock.stubFor(post(urlEqualTo("/chat/completions")).willReturn(aResponse().withStatus(429)));
            assertUnavailable();
            wireMock.stubFor(post(urlEqualTo("/chat/completions")).willReturn(json("{}").withFixedDelay(2000)));
            assertUnavailable();
        }

        @Test
        @DisplayName("no API key: AI_UNAVAILABLE without a request")
        void noKey() {
            properties.setApiKey(null);

            assertUnavailable();
            assertThat(wireMock.getAllServeEvents()).isEmpty();
        }
    }

    private void assertUnavailable() {
        assertThatThrownBy(() -> client.complete(request()))
                .isInstanceOfSatisfying(ServiceValidationException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_UNAVAILABLE);
                    assertThat(e.getMessage()).doesNotContain("sk-test");
                });
    }

    private static ChatCompletionRequest request() {
        return ChatCompletionRequest.builder()
                .model("m")
                .messages(List.of(ChatMessage.builder().role("user").content("hi").build()))
                .responseFormat(Map.of("type", "json_object"))
                .maxCompletionTokens(10)
                .build();
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(String body) {
        return aResponse().withHeader("Content-Type", "application/json").withBody(body);
    }
}
