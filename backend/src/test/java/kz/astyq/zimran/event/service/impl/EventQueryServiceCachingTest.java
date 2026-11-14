package kz.astyq.zimran.event.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.configuration.AppCacheProperties;
import kz.astyq.zimran.core.configuration.CacheConfig;
import kz.astyq.zimran.core.configuration.TicketmasterProperties;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.service.EventQueryService;
import kz.astyq.zimran.ticketmaster.client.TicketmasterClient;
import kz.astyq.zimran.ticketmaster.model.dto.TmEvent;
import kz.astyq.zimran.ticketmaster.service.InterestMappingService;
import kz.astyq.zimran.ticketmaster.service.TicketmasterEventNormalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Spring slice with the real {@link CacheConfig}: proves the caches exist with the configured names and that a
 * repeated search is served from them.
 */
@SpringJUnitConfig(classes = {CacheConfig.class, EventQueryServiceImpl.class,
        EventQueryServiceCachingTest.TestConfig.class})
class EventQueryServiceCachingTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final SupportedCity NYC = SupportedCity.builder()
            .id("nyc").name("New York").stateCode("NY").countryCode("US").zone(NEW_YORK).build();

    @Configuration
    @EnableConfigurationProperties({AppCacheProperties.class, TicketmasterProperties.class})
    static class TestConfig {

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @MockitoBean
    private TicketmasterClient ticketmasterClient;
    @MockitoBean
    private TicketmasterEventNormalizer ticketmasterEventNormalizer;
    @MockitoBean
    private InterestMappingService interestMappingService;
    @MockitoBean
    private SupportedCityService supportedCityService;

    @Autowired
    private EventQueryService eventQueryService;
    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("second identical findEvents does not call Ticketmaster; eventById is filled")
    void secondCallCached() {
        Event event = Event.builder()
                .id(new EventId(EventSource.TICKETMASTER, "tm1"))
                .title("Show")
                .sourceUrl("https://www.ticketmaster.com/event/tm1")
                .startAt(OffsetDateTime.parse("2026-11-20T20:00:00-05:00"))
                .timezone(NEW_YORK)
                .status(EventStatus.SCHEDULED)
                .category("Music")
                .interests(List.of())
                .build();
        when(ticketmasterClient.searchAllPages(any(), anyInt()))
                .thenReturn(List.of(TmEvent.builder().id("tm1").build()));
        when(ticketmasterEventNormalizer.normalize(any(), any())).thenReturn(Optional.of(event));
        LocalDate from = LocalDate.of(2026, 11, 20);

        List<Event> first = eventQueryService.findEvents(NYC, from, from.plusDays(1), Set.of());
        List<Event> second = eventQueryService.findEvents(NYC, from, from.plusDays(1), Set.of());

        assertThat(first).containsExactly(event);
        assertThat(second).containsExactly(event);
        verify(ticketmasterClient, times(1)).searchAllPages(any(), anyInt());
        assertThat(eventQueryService.findById(event.getId())).contains(event);
        assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
        assertThat(cacheManager.getCacheNames()).containsExactlyInAnyOrder(
                CacheConfig.EVENT_SEARCH, CacheConfig.EVENT_SEARCH_STALE, CacheConfig.EVENT_BY_ID);
    }
}
