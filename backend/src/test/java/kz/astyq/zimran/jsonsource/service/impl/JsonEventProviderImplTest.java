package kz.astyq.zimran.jsonsource.service.impl;

import kz.astyq.zimran.core.configuration.EventSourceProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.TestEvents;
import kz.astyq.zimran.jsonsource.client.EventJsonClient;
import kz.astyq.zimran.jsonsource.service.JsonEventNormalizer;
import kz.astyq.zimran.jsonsource.util.MessageCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonEventProviderImplTest {

    private static final JsonNode DOCUMENT = JsonNodeFactory.instance.arrayNode();

    @Mock
    private EventJsonClient client;
    @Mock
    private JsonEventNormalizer normalizer;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-14T12:00:00Z"));
    private JsonEventProviderImpl provider;

    @BeforeEach
    void setUp() {
        EventSourceProperties properties = new EventSourceProperties();
        properties.setRefreshInterval(Duration.ofMinutes(1));
        provider = new JsonEventProviderImpl(client, normalizer, properties, clock);
    }

    @Nested
    @DisplayName("getAll / findById")
    class Caching {

        @Test
        @DisplayName("the document is read once and served from memory within the refresh interval")
        void cached() {
            Event event = TestEvents.event("a").build();
            when(client.fingerprint()).thenReturn("v1");
            when(client.fetch()).thenReturn(DOCUMENT);
            when(normalizer.normalizeAll(DOCUMENT)).thenReturn(List.of(event));

            assertThat(provider.getAll()).containsExactly(event);
            clock.advance(Duration.ofSeconds(30));
            assertThat(provider.findById("a")).contains(event);
            assertThat(provider.findById("missing")).isEmpty();

            verify(client, times(1)).fetch();
        }

        @Test
        @DisplayName("after the interval: an unchanged file is not re-read, a changed one is")
        void reloadOnChange() {
            when(client.fingerprint()).thenReturn("v1", "v1", "v2");
            when(client.fetch()).thenReturn(DOCUMENT);
            when(normalizer.normalizeAll(DOCUMENT)).thenReturn(List.of(TestEvents.event("a").build()),
                    List.of(TestEvents.event("b").build()));

            provider.getAll();
            clock.advance(Duration.ofMinutes(2));
            assertThat(provider.getAll()).extracting(e -> e.getId().getSourceId()).containsExactly("a");
            clock.advance(Duration.ofMinutes(2));
            assertThat(provider.getAll()).extracting(e -> e.getId().getSourceId()).containsExactly("b");

            verify(client, times(2)).fetch();
        }

        @Test
        @DisplayName("a failed reload keeps serving the previous events")
        void staleOnFailure() {
            Event event = TestEvents.event("a").build();
            when(client.fingerprint()).thenReturn(null);
            when(client.fetch()).thenReturn(DOCUMENT).thenThrow(unavailable());
            when(normalizer.normalizeAll(DOCUMENT)).thenReturn(List.of(event));

            provider.getAll();
            clock.advance(Duration.ofMinutes(2));

            assertThat(provider.getAll()).containsExactly(event);
        }

        @Test
        @DisplayName("nothing loaded yet and the source fails: the error reaches the caller")
        void firstLoadFails() {
            when(client.fetch()).thenThrow(unavailable());

            assertThatThrownBy(provider::getAll)
                    .isInstanceOfSatisfying(ServiceValidationException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPSTREAM_UNAVAILABLE));
        }

        @Test
        @DisplayName("duplicate ids in the feed: the first record wins")
        void duplicateIds() {
            Event first = TestEvents.event("a").title("First").build();
            Event second = TestEvents.event("a").title("Second").build();
            when(client.fetch()).thenReturn(DOCUMENT);
            when(normalizer.normalizeAll(DOCUMENT)).thenReturn(List.of(first, second));

            assertThat(provider.getAll()).containsExactly(first);
            assertThat(provider.findById("a")).contains(first);
        }
    }

    private static ServiceValidationException unavailable() {
        return new ServiceValidationException(ErrorCode.UPSTREAM_UNAVAILABLE, MessageCode.EVENTS_SOURCE_UNAVAILABLE);
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
