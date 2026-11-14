package kz.astyq.zimran.event.job;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.service.CatalogRefreshService;
import kz.astyq.zimran.event.service.EventIngestionService;
import kz.astyq.zimran.event.util.MessageCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.util.List;

import static kz.astyq.zimran.event.model.domain.TestEvents.NYC;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventIngestionJobTest {

    private static final SupportedCity BOSTON = SupportedCity.builder()
            .id("boston-ma").name("Boston").stateCode("MA").zone(ZoneId.of("America/New_York")).build();

    @Mock
    private SupportedCityService supportedCityService;
    @Mock
    private CatalogRefreshService catalogRefreshService;
    @Mock
    private EventIngestionService eventIngestionService;
    @InjectMocks
    private EventIngestionJob job;

    @Nested
    @DisplayName("run")
    class Run {

        @Test
        @DisplayName("a failing city is skipped: the others are still ingested and past events expired")
        void cityFails() {
            when(supportedCityService.getAll()).thenReturn(List.of(NYC, BOSTON));
            when(catalogRefreshService.refresh(NYC)).thenThrow(new ServiceValidationException(
                    ErrorCode.UPSTREAM_UNAVAILABLE, MessageCode.EVENTS_SOURCE_UNAVAILABLE));
            when(catalogRefreshService.refresh(BOSTON)).thenReturn(List.of());

            assertThatCode(job::run).doesNotThrowAnyException();

            verify(catalogRefreshService).refresh(BOSTON);
            verify(eventIngestionService).expirePastEvents();
        }

        @Test
        @DisplayName("an unexpected error while expiring does not escape")
        void expireFails() {
            when(supportedCityService.getAll()).thenReturn(List.of());
            when(eventIngestionService.expirePastEvents()).thenThrow(new IllegalStateException("db down"));

            assertThatCode(job::run).doesNotThrowAnyException();
        }
    }
}
