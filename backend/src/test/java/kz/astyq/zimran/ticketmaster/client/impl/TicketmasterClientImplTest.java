package kz.astyq.zimran.ticketmaster.client.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import kz.astyq.zimran.core.configuration.TicketmasterClientConfig;
import kz.astyq.zimran.core.configuration.TicketmasterProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.ticketmaster.model.dto.TmClassification;
import kz.astyq.zimran.ticketmaster.model.dto.TmEvent;
import kz.astyq.zimran.ticketmaster.model.dto.TmPriceRange;
import kz.astyq.zimran.ticketmaster.model.dto.TmSearchQuery;
import kz.astyq.zimran.ticketmaster.model.dto.TmSearchResponse;
import kz.astyq.zimran.ticketmaster.model.dto.TmVenue;
import kz.astyq.zimran.ticketmaster.util.MessageCode;
import kz.astyq.zimran.ticketmaster.util.RequestThrottle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class TicketmasterClientImplTest {

    private static final String API_KEY = "SECRET-tm-key-123";
    private static final String SEARCH = "/discovery/v2/events.json";
    private static final String EVENT = "/discovery/v2/events/%s.json";

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private TicketmasterProperties properties;
    private TicketmasterClientImpl client;
    private Logger clientLogger;
    private Level previousLevel;

    @BeforeEach
    void setUp() {
        properties = new TicketmasterProperties();
        properties.setBaseUrl(wm.baseUrl());
        properties.setApiKey(API_KEY);
        properties.setReadTimeout(Duration.ofSeconds(5));
        client = newClient(new RequestThrottle(100, Duration.ofSeconds(1)));

        clientLogger = (Logger) LoggerFactory.getLogger(TicketmasterClientImpl.class);
        previousLevel = clientLogger.getLevel();
        clientLogger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void tearDown() {
        clientLogger.setLevel(previousLevel);
    }

    private TicketmasterClientImpl newClient(RequestThrottle throttle) {
        RestClient restClient = TicketmasterClientConfig.ticketmasterRestClient(RestClient.builder(), properties);
        return new TicketmasterClientImpl(properties, restClient, throttle);
    }

    @Nested
    @DisplayName("searchEvents")
    class SearchEvents {

        @Test
        @DisplayName("sends every search parameter, apikey and fixed sort/locale")
        void sendsParams() {
            wm.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(okJson(fixture("search-nyc-music.json"))));

            client.searchEvents(nycMusic().build());

            wm.verify(1, getRequestedFor(urlPathEqualTo(SEARCH))
                    .withQueryParam("city", equalTo("New York"))
                    .withQueryParam("stateCode", equalTo("NY"))
                    .withQueryParam("countryCode", equalTo("US"))
                    .withQueryParam("startDateTime", equalTo("2026-11-20T05:00:00Z"))
                    .withQueryParam("endDateTime", equalTo("2026-11-23T04:59:59Z"))
                    .withQueryParam("classificationName", equalTo("Music"))
                    .withQueryParam("page", equalTo("0"))
                    .withQueryParam("size", equalTo("50"))
                    .withQueryParam("sort", equalTo("date,asc"))
                    .withQueryParam("locale", equalTo("*"))
                    .withQueryParam("apikey", equalTo(API_KEY)));
        }

        @Test
        @DisplayName("omits null parameters, encodes values and caps size at 200")
        void omitsNullParamsAndCapsSize() {
            wm.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(okJson(fixture("search-empty.json"))));

            client.searchEvents(TmSearchQuery.builder().city("New York & Co").page(2).size(500).build());

            wm.verify(1, getRequestedFor(urlPathEqualTo(SEARCH))
                    .withQueryParam("city", equalTo("New York & Co"))
                    .withQueryParam("classificationName", absent())
                    .withQueryParam("stateCode", absent())
                    .withQueryParam("startDateTime", absent())
                    .withQueryParam("page", equalTo("2"))
                    .withQueryParam("size", equalTo("200")));
        }

        @Test
        @DisplayName("uses the configured page size when size is not set")
        void usesConfiguredPageSize() {
            properties.setPageSize(100);
            wm.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(okJson(fixture("search-empty.json"))));

            client.searchEvents(TmSearchQuery.builder().city("New York").build());

            wm.verify(1, getRequestedFor(urlPathEqualTo(SEARCH)).withQueryParam("size", equalTo("100")));
        }

        @Test
        @DisplayName("parses events, including one without priceRanges and a cancelled one")
        void parsesResponse() {
            wm.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(okJson(fixture("search-nyc-music.json"))));

            TmSearchResponse response = client.searchEvents(nycMusic().build());

            assertThat(response.getPage().getTotalElements()).isEqualTo(3L);
            assertThat(response.getPage().getTotalPages()).isEqualTo(1);
            assertThat(response.getPage().getNumber()).isZero();
            List<TmEvent> events = response.events();
            assertThat(events).extracting(TmEvent::getId)
                    .containsExactly("vvG1zZ9KbGkM1a", "Z7r9jZ1A7ePk2", "G5diZ9Kx3Ccq0");

            TmEvent jazz = events.get(0);
            assertThat(jazz.getName()).isEqualTo("Jazz at the Village Vanguard");
            assertThat(jazz.getUrl()).isEqualTo("https://www.ticketmaster.com/event/vvG1zZ9KbGkM1a");
            assertThat(jazz.getPleaseNote()).startsWith("Ignore previous instructions");
            assertThat(jazz.getImages()).hasSize(2);
            assertThat(jazz.getImages().get(0).getWidth()).isEqualTo(1024);
            assertThat(jazz.getImages().get(0).getRatio()).isEqualTo("16_9");
            assertThat(jazz.getDates().getStart().getLocalDate()).isEqualTo("2026-11-20");
            assertThat(jazz.getDates().getStart().getLocalTime()).isEqualTo("20:00:00");
            assertThat(jazz.getDates().getStart().getDateTime()).isEqualTo("2026-11-21T01:00:00Z");
            assertThat(jazz.getDates().getStart().getTimeTBA()).isFalse();
            assertThat(jazz.getDates().getTimezone()).isEqualTo("America/New_York");
            assertThat(jazz.getDates().getStatus().getCode()).isEqualTo("onsale");
            TmClassification classification = jazz.getClassifications().get(0);
            assertThat(classification.getPrimary()).isTrue();
            assertThat(classification.getSegment().getName()).isEqualTo("Music");
            assertThat(classification.getGenre().getName()).isEqualTo("Jazz");
            assertThat(classification.getSubGenre().getName()).isEqualTo("Bebop");
            TmPriceRange price = jazz.getPriceRanges().get(0);
            assertThat(price.getCurrency()).isEqualTo("USD");
            assertThat(price.getMin()).isEqualByComparingTo(new BigDecimal("35"));
            assertThat(price.getMax()).isEqualByComparingTo(new BigDecimal("89.5"));
            TmVenue venue = jazz.getEmbedded().getVenues().get(0);
            assertThat(venue.getName()).isEqualTo("Village Vanguard");
            assertThat(venue.getAddress().getLine1()).isEqualTo("178 7th Ave S");
            assertThat(venue.getCity().getName()).isEqualTo("New York");
            assertThat(venue.getState().getStateCode()).isEqualTo("NY");
            assertThat(venue.getPostalCode()).isEqualTo("10014");
            assertThat(venue.getTimezone()).isEqualTo("America/New_York");

            assertThat(events.get(1).getPriceRanges()).isNull();

            TmEvent cancelled = events.get(2);
            assertThat(cancelled.getDates().getStatus().getCode()).isEqualTo("cancelled");
            assertThat(cancelled.getDates().getStart().getLocalTime()).isNull();
            assertThat(cancelled.getDates().getStart().getDateTime()).isNull();
            assertThat(cancelled.getDates().getStart().getNoSpecificTime()).isTrue();
            assertThat(cancelled.getImages()).isEmpty();
        }

        @Test
        @DisplayName("empty result without _embedded has no events")
        void emptyResult() {
            wm.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(okJson(fixture("search-empty.json"))));

            TmSearchResponse response = client.searchEvents(nycMusic().build());

            assertThat(response.getEmbedded()).isNull();
            assertThat(response.events()).isEmpty();
            assertThat(response.getPage().getTotalElements()).isZero();
        }

        @Test
        @DisplayName("429 then 200: retried, waiting for Retry-After")
        void retriesAfter429() {
            wm.stubFor(get(urlPathEqualTo(SEARCH)).inScenario("rate").whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "1"))
                    .willSetStateTo("ok"));
            wm.stubFor(get(urlPathEqualTo(SEARCH)).inScenario("rate").whenScenarioStateIs("ok")
                    .willReturn(okJson(fixture("search-nyc-music.json"))));

            long start = System.nanoTime();
            TmSearchResponse response = client.searchEvents(nycMusic().build());
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            assertThat(response.events()).hasSize(3);
            wm.verify(2, getRequestedFor(urlPathEqualTo(SEARCH)));
            assertThat(elapsedMs).as("waited for Retry-After: 1s, not the 300 ms backoff").isGreaterThanOrEqualTo(950);
        }

        @Test
        @DisplayName("Retry-After above 2 s is capped")
        void capsRetryAfter() {
            wm.stubFor(get(urlPathEqualTo(SEARCH)).inScenario("cap").whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse().withStatus(503).withHeader("Retry-After", "60"))
                    .willSetStateTo("ok"));
            wm.stubFor(get(urlPathEqualTo(SEARCH)).inScenario("cap").whenScenarioStateIs("ok")
                    .willReturn(okJson(fixture("search-empty.json"))));

            long start = System.nanoTime();
            client.searchEvents(nycMusic().build());
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            assertThat(elapsedMs).isBetween(1900L, 5000L);
        }

        @Test
        @DisplayName("500 three times: UPSTREAM_UNAVAILABLE after 300 ms + 900 ms backoff")
        void persistent500() {
            wm.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(aResponse().withStatus(500)));

            long start = System.nanoTime();
            assertThatThrownBy(() -> client.searchEvents(nycMusic().build()))
                    .isInstanceOfSatisfying(ServiceValidationException.class, e -> {
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPSTREAM_UNAVAILABLE);
                        assertThat(e.getMessageCode()).isEqualTo(MessageCode.TICKETMASTER_UNAVAILABLE);
                        assertThat(e.getStatus().value()).isEqualTo(503);
                    });
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            wm.verify(3, getRequestedFor(urlPathEqualTo(SEARCH)));
            assertThat(elapsedMs).as("300 ms + 900 ms backoff").isGreaterThanOrEqualTo(1150);
        }

        @Test
        @DisplayName("read timeout: retried, then UPSTREAM_UNAVAILABLE without the key in the exception")
        void readTimeout(CapturedOutput output) {
            properties.setReadTimeout(Duration.ofMillis(200));
            client = newClient(new RequestThrottle(100, Duration.ofSeconds(1)));
            wm.stubFor(get(urlPathEqualTo(SEARCH))
                    .willReturn(okJson(fixture("search-empty.json")).withFixedDelay(1500)));

            assertThatThrownBy(() -> client.searchEvents(nycMusic().build()))
                    .isInstanceOfSatisfying(ServiceValidationException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPSTREAM_UNAVAILABLE))
                    .satisfies(e -> assertThat(fullText(e)).doesNotContain(API_KEY));

            // WireMock does not journal requests whose client hung up before the delayed response, so
            // the attempts are counted from the client's per-attempt WARN lines.
            String logs = output.getAll();
            for (int attempt = 1; attempt <= 3; attempt++) {
                assertThat(logs).containsPattern(
                        "WARN.*GET /discovery/v2/events\\.json\\?.*failed in \\d+ ms \\(attempt " + attempt
                                + "\\): HttpTimeoutException");
            }
            assertThat(logs).doesNotContain("(attempt 4)").doesNotContain(API_KEY);
        }

        @Test
        @DisplayName("401: logged at ERROR, not retried")
        void unauthorized(CapturedOutput output) {
            wm.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(aResponse().withStatus(401)));

            assertThatThrownBy(() -> client.searchEvents(nycMusic().build()))
                    .isInstanceOfSatisfying(ServiceValidationException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPSTREAM_UNAVAILABLE));

            wm.verify(1, getRequestedFor(urlPathEqualTo(SEARCH)));
            assertThat(output.getAll()).containsPattern("ERROR.*Ticketmaster rejected API key");
        }

        @Test
        @DisplayName("malformed JSON is not retried")
        void malformedJson() {
            wm.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(okJson("{\"page\": [")));

            assertThatThrownBy(() -> client.searchEvents(nycMusic().build()))
                    .isInstanceOf(ServiceValidationException.class);

            wm.verify(1, getRequestedFor(urlPathEqualTo(SEARCH)));
        }

        @Test
        @DisplayName("blank API key: TICKETMASTER_NOT_CONFIGURED, no HTTP call")
        void blankApiKey() {
            properties.setApiKey("  ");

            assertThatThrownBy(() -> client.searchEvents(nycMusic().build()))
                    .isInstanceOfSatisfying(ServiceValidationException.class, e -> {
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPSTREAM_UNAVAILABLE);
                        assertThat(e.getMessageCode()).isEqualTo(MessageCode.TICKETMASTER_NOT_CONFIGURED);
                    });
            assertThat(wm.getAllServeEvents()).isEmpty();
        }

        @Test
        @DisplayName("throttle: fails fast when no permit within max wait")
        void throttled() {
            client = newClient(new RequestThrottle(1, Duration.ZERO, () -> 0L, nanos -> { }));
            wm.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(okJson(fixture("search-empty.json"))));

            client.searchEvents(nycMusic().build()); // takes the only permit
            assertThatThrownBy(() -> client.searchEvents(nycMusic().build()))
                    .isInstanceOfSatisfying(ServiceValidationException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPSTREAM_UNAVAILABLE));

            wm.verify(1, getRequestedFor(urlPathEqualTo(SEARCH)));
        }

        @Test
        @DisplayName("API key is masked in logs")
        void apiKeyMaskedInLogs(CapturedOutput output) {
            wm.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(okJson(fixture("search-empty.json"))));
            wm.stubFor(get(urlPathEqualTo(EVENT.formatted("boom"))).willReturn(aResponse().withStatus(503)));

            client.searchEvents(nycMusic().build());
            assertThatThrownBy(() -> client.getEvent("boom")).isInstanceOf(ServiceValidationException.class);

            String logs = output.getAll();
            assertThat(logs)
                    .doesNotContain(API_KEY)
                    .containsPattern("DEBUG.*GET /discovery/v2/events\\.json\\?city=New York.*apikey=\\*\\*\\* -> 200 "
                            + "in \\d+ ms \\(attempt 1\\)")
                    .containsPattern("WARN.*/discovery/v2/events/boom\\.json.*503.*attempt 3");
        }
    }

    @Nested
    @DisplayName("getEvent")
    class GetEvent {

        @Test
        @DisplayName("sends locale and apikey, parses the event")
        void parsesDetail() {
            wm.stubFor(get(urlPathEqualTo(EVENT.formatted("vvG1zZ9KbGkM1a")))
                    .willReturn(okJson(fixture("event-detail.json"))));

            Optional<TmEvent> event = client.getEvent("vvG1zZ9KbGkM1a");

            assertThat(event).isPresent();
            assertThat(event.get().getId()).isEqualTo("vvG1zZ9KbGkM1a");
            assertThat(event.get().getDescription()).contains("bebop");
            assertThat(event.get().getEmbedded().getVenues().get(0).getName()).isEqualTo("Village Vanguard");
            wm.verify(getRequestedFor(urlPathEqualTo(EVENT.formatted("vvG1zZ9KbGkM1a")))
                    .withQueryParam("locale", equalTo("*"))
                    .withQueryParam("apikey", equalTo(API_KEY)));
        }

        @Test
        @DisplayName("404: Optional.empty, not retried")
        void notFound() {
            wm.stubFor(get(urlPathEqualTo(EVENT.formatted("missing"))).willReturn(aResponse().withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"errors\":[{\"code\":\"DIS1004\",\"detail\":\"Resource not found\"}]}")));

            assertThat(client.getEvent("missing")).isEmpty();
            wm.verify(1, getRequestedFor(urlPathEqualTo(EVENT.formatted("missing"))));
        }

        @Test
        @DisplayName("403: UPSTREAM_UNAVAILABLE, not retried")
        void forbidden() {
            wm.stubFor(get(urlPathEqualTo(EVENT.formatted("x1"))).willReturn(aResponse().withStatus(403)));

            assertThatThrownBy(() -> client.getEvent("x1"))
                    .isInstanceOfSatisfying(ServiceValidationException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPSTREAM_UNAVAILABLE));
            wm.verify(1, getRequestedFor(urlPathEqualTo(EVENT.formatted("x1"))));
        }

        @Test
        @DisplayName("blank API key: TICKETMASTER_NOT_CONFIGURED, no HTTP call")
        void blankApiKey() {
            properties.setApiKey("");

            assertThatThrownBy(() -> client.getEvent("vvG1zZ9KbGkM1a"))
                    .isInstanceOfSatisfying(ServiceValidationException.class,
                            e -> assertThat(e.getMessageCode()).isEqualTo(MessageCode.TICKETMASTER_NOT_CONFIGURED));
            assertThat(wm.getAllServeEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchAllPages")
    class SearchAllPages {

        @Test
        @DisplayName("stops at the deep-paging cap: (page + 1) * size >= 1000")
        void stopsAtDeepPagingLimit() {
            for (int page = 0; page < 10; page++) {
                stubPage(page, 50);
            }

            List<TmEvent> events = client.searchAllPages(nycMusic().size(200).build(), 10);

            assertThat(events).extracting(TmEvent::getId).containsExactly("e0", "e1", "e2", "e3", "e4");
            wm.verify(5, getRequestedFor(urlPathEqualTo(SEARCH)));
            wm.verify(0, getRequestedFor(urlPathEqualTo(SEARCH)).withQueryParam("page", equalTo("5")));
        }

        @Test
        @DisplayName("stops at maxPages")
        void stopsAtMaxPages() {
            for (int page = 0; page < 5; page++) {
                stubPage(page, 50);
            }

            List<TmEvent> events = client.searchAllPages(nycMusic().size(20).build(), 2);

            assertThat(events).extracting(TmEvent::getId).containsExactly("e0", "e1");
            wm.verify(2, getRequestedFor(urlPathEqualTo(SEARCH)));
        }

        @Test
        @DisplayName("stops at the last page")
        void stopsAtLastPage() {
            for (int page = 0; page < 5; page++) {
                stubPage(page, 3);
            }

            List<TmEvent> events = client.searchAllPages(nycMusic().size(20).build(), 10);

            assertThat(events).extracting(TmEvent::getId).containsExactly("e0", "e1", "e2");
            wm.verify(3, getRequestedFor(urlPathEqualTo(SEARCH)));
        }

        @Test
        @DisplayName("empty result: one call, no events")
        void emptyResult() {
            wm.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(okJson(fixture("search-empty.json"))));

            assertThat(client.searchAllPages(nycMusic().build(), 5)).isEmpty();
            wm.verify(1, getRequestedFor(urlPathEqualTo(SEARCH)));
        }

        @Test
        @DisplayName("blank API key: TICKETMASTER_NOT_CONFIGURED, no HTTP call")
        void blankApiKey() {
            properties.setApiKey(null);

            assertThatThrownBy(() -> client.searchAllPages(nycMusic().build(), 3))
                    .isInstanceOfSatisfying(ServiceValidationException.class,
                            e -> assertThat(e.getMessageCode()).isEqualTo(MessageCode.TICKETMASTER_NOT_CONFIGURED));
            assertThat(wm.getAllServeEvents()).isEmpty();
        }

        private void stubPage(int page, int totalPages) {
            String body = """
                    {"_embedded":{"events":[{"id":"e%d","name":"Event %d"}]},
                     "page":{"size":1,"totalElements":%d,"totalPages":%d,"number":%d}}
                    """.formatted(page, page, totalPages, totalPages, page);
            wm.stubFor(get(urlPathEqualTo(SEARCH)).withQueryParam("page", equalTo(String.valueOf(page)))
                    .willReturn(okJson(body)));
        }
    }

    @Nested
    @DisplayName("parseRetryAfter")
    class ParseRetryAfter {

        @Test
        @DisplayName("accepts delta-seconds and HTTP-date, ignores garbage")
        void parses() {
            assertThat(TicketmasterClientImpl.parseRetryAfter(null)).isNull();
            assertThat(TicketmasterClientImpl.parseRetryAfter("")).isNull();
            assertThat(TicketmasterClientImpl.parseRetryAfter("2")).isEqualTo(Duration.ofSeconds(2));
            assertThat(TicketmasterClientImpl.parseRetryAfter("-1")).isNull();
            assertThat(TicketmasterClientImpl.parseRetryAfter("soon")).isNull();
            assertThat(TicketmasterClientImpl.parseRetryAfter("Wed, 21 Oct 2015 07:28:00 GMT"))
                    .isEqualTo(Duration.ZERO);
        }
    }

    private static TmSearchQuery.TmSearchQueryBuilder nycMusic() {
        return TmSearchQuery.builder()
                .city("New York")
                .stateCode("NY")
                .countryCode("US")
                .start(Instant.parse("2026-11-20T05:00:00.123Z"))
                .end(Instant.parse("2026-11-23T04:59:59Z"))
                .classificationName("Music")
                .page(0)
                .size(50);
    }

    private static String fullText(Throwable e) {
        StringBuilder text = new StringBuilder();
        for (Throwable t = e; t != null; t = t.getCause()) {
            text.append(t).append('\n');
        }
        return text.toString();
    }

    private static String fixture(String name) {
        try {
            return new ClassPathResource("ticketmaster/" + name).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
