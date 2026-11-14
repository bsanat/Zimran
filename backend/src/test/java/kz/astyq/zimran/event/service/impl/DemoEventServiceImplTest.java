package kz.astyq.zimran.event.service.impl;

import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.service.EventIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static kz.astyq.zimran.event.model.domain.TestEvents.NYC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoEventServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-14T17:00:00Z");

    @Mock
    private SupportedCityService supportedCityService;
    @Mock
    private EventIngestionService eventIngestionService;

    private DemoEventServiceImpl service;

    @BeforeEach
    void setUp() {
        when(supportedCityService.getDefault()).thenReturn(NYC);
        service = new DemoEventServiceImpl(JsonMapper.builder().build(), supportedCityService, eventIngestionService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Nested
    @DisplayName("sampleEvents")
    class SampleEvents {

        @Test
        @DisplayName("clearly fictional NYC events: provider demo, isDemo, no source URL or image, upcoming")
        void fictional() {
            List<Event> events = service.sampleEvents();

            assertThat(events).isNotEmpty().allSatisfy(event -> {
                assertThat(event.getId().getSource()).isEqualTo(EventSource.DEMO);
                assertThat(event.isDemo()).isTrue();
                assertThat(event.getTitle()).startsWith("[Demo]");
                assertThat(event.getSourceUrl()).isNull();
                assertThat(event.getImageUrl()).isNull();
                assertThat(event.getCity()).isEqualTo("New York");
                assertThat(event.getTimezone()).isEqualTo(NYC.getZone());
                assertThat(event.getLocalDate()).isAfter(LocalDate.of(2026, 11, 14));
                assertThat(event.getInterests()).contains(event.getCategory());
            });
        }

        @Test
        @DisplayName("a sample without price stays unknown, never free")
        void unknownPrice() {
            Event unknown = service.sampleEvents().stream().filter(e -> !e.isPriceKnown()).findFirst().orElseThrow();

            assertThat(unknown.getPriceMin()).isNull();
            assertThat(unknown.getPriceMax()).isNull();
            assertThat(unknown.getCurrency()).isNull();
        }
    }

    @Test
    @DisplayName("seed: upserts every sample")
    void seed() {
        int seeded = service.seed();

        assertThat(seeded).isEqualTo(service.sampleEvents().size());
        verify(eventIngestionService).upsert(org.mockito.ArgumentMatchers.argThat(list -> list.size() == seeded));
    }
}
