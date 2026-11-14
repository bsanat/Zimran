package kz.astyq.zimran.jsonsource.client.impl;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import kz.astyq.zimran.core.configuration.EventSourceProperties;
import kz.astyq.zimran.core.configuration.HttpClientConfig;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.jsonsource.util.MessageCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventJsonClientImplTest {

    private final EventSourceProperties properties = new EventSourceProperties();
    private EventJsonClientImpl client;
    private WireMockServer wireMock;

    @BeforeEach
    void setUp() {
        RestClient restClient = HttpClientConfig.restClient(RestClient.builder(), Duration.ofSeconds(1),
                Duration.ofMillis(500), null);
        client = new EventJsonClientImpl(properties, restClient, JsonMapper.builder().build());
    }

    @AfterEach
    void tearDown() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @Nested
    @DisplayName("file")
    class FileSource {

        @Test
        @DisplayName("reads and parses the file; fingerprint changes with the content")
        void reads(@TempDir Path dir) throws IOException {
            Path file = dir.resolve("events.json");
            Files.writeString(file, "[{\"id\":\"a\"}]");
            properties.setLocation(file.toString());

            String before = client.fingerprint();
            assertThat(client.fetch().get(0).get("id").stringValue()).isEqualTo("a");
            Files.writeString(file, "[{\"id\":\"a\"},{\"id\":\"b\"}]");

            assertThat(before).isNotNull().isNotEqualTo(client.fingerprint());
        }

        @Test
        @DisplayName("missing file, invalid JSON or too large: UPSTREAM_UNAVAILABLE")
        void failures(@TempDir Path dir) throws IOException {
            properties.setLocation(dir.resolve("missing.json").toString());
            assertUnavailable(MessageCode.EVENTS_SOURCE_UNAVAILABLE);
            assertThat(client.fingerprint()).isNull();

            Path broken = dir.resolve("broken.json");
            Files.writeString(broken, "{not json");
            properties.setLocation(broken.toString());
            assertUnavailable(MessageCode.EVENTS_SOURCE_UNAVAILABLE);

            Path big = dir.resolve("big.json");
            Files.writeString(big, "[" + "1,".repeat(100) + "1]");
            properties.setLocation(big.toString());
            properties.setMaxBytes(50);
            assertUnavailable(MessageCode.EVENTS_SOURCE_UNAVAILABLE);
        }

        @Test
        @DisplayName("not configured: UPSTREAM_UNAVAILABLE with the not-configured message")
        void notConfigured() {
            assertThat(client.isConfigured()).isFalse();
            assertUnavailable(MessageCode.EVENTS_SOURCE_NOT_CONFIGURED);
        }
    }

    @Nested
    @DisplayName("url")
    class UrlSource {

        @BeforeEach
        void startServer() {
            wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
            wireMock.start();
        }

        @Test
        @DisplayName("downloads and parses; no cheap fingerprint")
        void downloads() {
            wireMock.stubFor(get(urlEqualTo("/events.json")).willReturn(aResponse()
                    .withHeader("Content-Type", "application/json").withBody("{\"events\":[]}")));
            properties.setLocation(wireMock.baseUrl() + "/events.json");

            assertThat(client.fetch().get("events").isArray()).isTrue();
            assertThat(client.fingerprint()).isNull();
        }

        @Test
        @DisplayName("HTTP error or a read timeout: UPSTREAM_UNAVAILABLE")
        void failures() {
            wireMock.stubFor(get(urlEqualTo("/down")).willReturn(aResponse().withStatus(500)));
            wireMock.stubFor(get(urlEqualTo("/slow")).willReturn(aResponse().withBody("[]").withFixedDelay(2000)));

            properties.setLocation(wireMock.baseUrl() + "/down");
            assertUnavailable(MessageCode.EVENTS_SOURCE_UNAVAILABLE);
            properties.setLocation(wireMock.baseUrl() + "/slow");
            assertUnavailable(MessageCode.EVENTS_SOURCE_UNAVAILABLE);
        }
    }

    private void assertUnavailable(String messageCode) {
        assertThatThrownBy(client::fetch).isInstanceOfSatisfying(ServiceValidationException.class, e -> {
            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPSTREAM_UNAVAILABLE);
            assertThat(e.getMessageCode()).isEqualTo(messageCode);
        });
    }
}
