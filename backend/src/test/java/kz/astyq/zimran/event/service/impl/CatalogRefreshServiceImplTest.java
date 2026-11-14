package kz.astyq.zimran.event.service.impl;

import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.MutableClock;
import kz.astyq.zimran.core.configuration.DemoProperties;
import kz.astyq.zimran.core.configuration.IngestionProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.domain.NewEventsIngestedEvent;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.repository.CatalogEventRepository;
import kz.astyq.zimran.event.service.EventIngestionService;
import kz.astyq.zimran.event.service.EventProvider;
import kz.astyq.zimran.event.util.MessageCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static kz.astyq.zimran.event.model.domain.TestEvents.NYC;
import static kz.astyq.zimran.event.model.domain.TestEvents.event;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogRefreshServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");

    @Mock
    private EventProvider provider;
    @Mock
    private EventIngestionService eventIngestionService;
    @Mock
    private CatalogEventRepository repository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private SupportedCityService supportedCityService;

    private final IngestionProperties ingestionProperties = new IngestionProperties();
    private final DemoProperties demoProperties = new DemoProperties();
    private MutableClock clock;
    private CatalogRefreshServiceImpl service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(NOW);
        lenient().when(provider.source()).thenReturn(EventSource.JSON);
        service = new CatalogRefreshServiceImpl(List.of(provider), eventIngestionService, repository, eventPublisher,
                ingestionProperties, demoProperties, supportedCityService, clock);
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("upserts only the city's events and publishes the inserted ones")
        void publishesInserted() {
            Event ny = event("ny").build();
            Event boston = event("boston").city("Boston").build();
            when(provider.getAll()).thenReturn(List.of(ny, boston));
            when(eventIngestionService.upsert(List.of(ny))).thenReturn(List.of(ny));

            assertThat(service.refresh(NYC)).containsExactly(ny);

            ArgumentCaptor<NewEventsIngestedEvent> published = ArgumentCaptor.forClass(NewEventsIngestedEvent.class);
            verify(eventPublisher).publishEvent(published.capture());
            assertThat(published.getValue().getCity()).isEqualTo(NYC);
            assertThat(published.getValue().getEvents()).containsExactly(ny);
        }

        @Test
        @DisplayName("nothing new: nothing published")
        void nothingNew() {
            when(provider.getAll()).thenReturn(List.of(event("ny").build()));
            when(eventIngestionService.upsert(anyList())).thenReturn(List.of());

            assertThat(service.refresh(NYC)).isEmpty();
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("a failing provider is skipped while another one works")
        void oneProviderFails() {
            EventProvider broken = mock(EventProvider.class);
            when(broken.source()).thenReturn(EventSource.JSON);
            when(broken.getAll()).thenThrow(new IllegalStateException("https://api.example?apikey=secret"));
            Event ny = event("ny").build();
            when(provider.getAll()).thenReturn(List.of(ny));
            service = new CatalogRefreshServiceImpl(List.of(broken, provider), eventIngestionService, repository,
                    eventPublisher, ingestionProperties, demoProperties, supportedCityService, clock);

            service.refresh(NYC);

            verify(eventIngestionService).upsert(List.of(ny));
        }

        @Test
        @DisplayName("every provider fails: UPSTREAM_UNAVAILABLE, nothing written")
        void allFail() {
            when(provider.getAll()).thenThrow(new IllegalStateException("boom"));

            assertThatThrownBy(() -> service.refresh(NYC))
                    .isInstanceOfSatisfying(ServiceValidationException.class, e -> {
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPSTREAM_UNAVAILABLE);
                        assertThat(e.getMessageCode()).isEqualTo(MessageCode.EVENTS_SOURCE_UNAVAILABLE);
                    });
            verify(eventIngestionService, never()).upsert(anyList());
        }
    }

    @Nested
    @DisplayName("ensureLoaded")
    class EnsureLoaded {

        @Test
        @DisplayName("the city has provider rows: no fetch")
        @SuppressWarnings("unchecked")
        void loaded() {
            when(repository.exists(any(Specification.class))).thenReturn(true);

            service.ensureLoaded(NYC);

            verify(provider, never()).getAll();
        }

        @Test
        @DisplayName("empty city: fetched live, then not again within the cooldown")
        @SuppressWarnings("unchecked")
        void cooldown() {
            when(repository.exists(any(Specification.class))).thenReturn(false);
            when(provider.getAll()).thenReturn(List.of());

            service.ensureLoaded(NYC);
            service.ensureLoaded(NYC);
            verify(provider, times(1)).getAll();

            clock.set(NOW.plus(ingestionProperties.getLiveFetchCooldown()).plusSeconds(1));
            service.ensureLoaded(NYC);
            verify(provider, times(2)).getAll();
        }

        @Test
        @DisplayName("fetch fails and the city has no rows at all: the error reaches the caller")
        @SuppressWarnings("unchecked")
        void failsWhenEmpty() {
            when(repository.exists(any(Specification.class))).thenReturn(false);
            when(provider.getAll()).thenThrow(unavailable());

            assertThatThrownBy(() -> service.ensureLoaded(NYC)).isInstanceOf(ServiceValidationException.class);
            // No cooldown after a failure: the next request tries again.
            assertThatThrownBy(() -> service.ensureLoaded(NYC)).isInstanceOf(ServiceValidationException.class);
            verify(provider, times(2)).getAll();
        }

        @Test
        @DisplayName("fetch fails but demo rows exist: the stored catalog is served")
        @SuppressWarnings("unchecked")
        void failsWithDemoRows() {
            demoProperties.setEnabled(true);
            when(repository.exists(any(Specification.class))).thenReturn(false, true);
            when(provider.getAll()).thenThrow(unavailable());

            service.ensureLoaded(NYC);
        }
    }

    @Nested
    @DisplayName("importById")
    class ImportById {

        @Test
        @DisplayName("found at the provider: stored and announced")
        void found() {
            Event event = event("e1").state("NY").build();
            when(provider.findById("e1")).thenReturn(Optional.of(event));
            when(eventIngestionService.upsert(List.of(event))).thenReturn(List.of(event));
            when(supportedCityService.findByName("New York", "NY")).thenReturn(Optional.of(NYC));

            assertThat(service.importById(new EventId(EventSource.JSON, "e1"))).contains(event);
            verify(eventPublisher).publishEvent(any(NewEventsIngestedEvent.class));
        }

        @Test
        @DisplayName("no provider for the source (demo): empty")
        void noProvider() {
            assertThat(service.importById(new EventId(EventSource.DEMO, "x"))).isEmpty();
            verify(eventIngestionService, never()).upsert(anyList());
        }
    }

    private static ServiceValidationException unavailable() {
        return new ServiceValidationException(ErrorCode.UPSTREAM_UNAVAILABLE, MessageCode.EVENTS_SOURCE_UNAVAILABLE);
    }
}
