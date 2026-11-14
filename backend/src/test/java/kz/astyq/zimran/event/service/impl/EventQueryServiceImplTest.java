package kz.astyq.zimran.event.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.configuration.AppCacheProperties;
import kz.astyq.zimran.core.configuration.CacheConfig;
import kz.astyq.zimran.core.configuration.TicketmasterProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.ticketmaster.client.TicketmasterClient;
import kz.astyq.zimran.ticketmaster.model.dto.TmEvent;
import kz.astyq.zimran.ticketmaster.model.dto.TmEventEmbedded;
import kz.astyq.zimran.ticketmaster.model.dto.TmNamed;
import kz.astyq.zimran.ticketmaster.model.dto.TmSearchQuery;
import kz.astyq.zimran.ticketmaster.model.dto.TmVenue;
import kz.astyq.zimran.ticketmaster.service.InterestMappingService;
import kz.astyq.zimran.ticketmaster.service.TicketmasterEventNormalizer;
import kz.astyq.zimran.ticketmaster.util.MessageCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventQueryServiceImplTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final SupportedCity NYC = SupportedCity.builder()
            .id("nyc").name("New York").stateCode("NY").countryCode("US").zone(NEW_YORK).build();
    /** Saturday 2026-03-07 10:30 in New York (EST); DST starts on Sunday 2026-03-08. */
    private static final Instant NOW = Instant.parse("2026-03-07T15:30:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 7);

    @Mock
    private TicketmasterClient ticketmasterClient;
    @Mock
    private TicketmasterEventNormalizer ticketmasterEventNormalizer;
    @Mock
    private InterestMappingService interestMappingService;
    @Mock
    private SupportedCityService supportedCityService;

    private CacheManager cacheManager;
    private EventQueryServiceImpl service;

    /** What the mocked normalizer returns for each Ticketmaster id. */
    private final Map<String, Event> normalized = new HashMap<>();

    @BeforeEach
    void setUp() {
        TicketmasterProperties ticketmasterProperties = new TicketmasterProperties();
        cacheManager = CacheConfig.buildCacheManager(new AppCacheProperties());
        service = new EventQueryServiceImpl(ticketmasterClient, ticketmasterEventNormalizer, interestMappingService,
                supportedCityService, ticketmasterProperties, cacheManager, Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(ticketmasterEventNormalizer.normalize(any(), any()))
                .thenAnswer(inv -> Optional.ofNullable(normalized.get(inv.<TmEvent>getArgument(0).getId())));
    }

    @Nested
    @DisplayName("findEvents")
    class FindEvents {

        @Test
        @DisplayName("future range: city-local midnight to the midnight after endDate, across the DST switch")
        void dstRange() {
            stubSearch();

            service.findEvents(NYC, LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 8), Set.of());

            TmSearchQuery query = capturedQuery();
            assertThat(query.getStart()).isEqualTo(Instant.parse("2026-03-08T05:00:00Z"));
            assertThat(query.getEnd()).isEqualTo(Instant.parse("2026-03-09T04:00:00Z"));
            assertThat(query.getCity()).isEqualTo("New York");
            assertThat(query.getStateCode()).isEqualTo("NY");
            assertThat(query.getCountryCode()).isEqualTo("US");
            assertThat(query.getClassificationName()).isNull();
            assertThat(query.getSize()).isEqualTo(EventQueryServiceImpl.SEARCH_PAGE_SIZE);
            verify(ticketmasterClient).searchAllPages(any(), eq(2));
        }

        @Test
        @DisplayName("range starting today: starts now (top of the hour), ends after endDate")
        void startsNow() {
            stubSearch();

            service.findEvents(NYC, TODAY, TODAY.plusDays(1), Set.of());

            TmSearchQuery query = capturedQuery();
            assertThat(query.getStart()).isEqualTo(Instant.parse("2026-03-07T15:00:00Z"));
            assertThat(query.getEnd()).isEqualTo(Instant.parse("2026-03-09T04:00:00Z"));
        }

        @Test
        @DisplayName("range already over or reversed: empty, no Ticketmaster call")
        void emptyWindow() {
            assertThat(service.findEvents(NYC, TODAY.minusDays(3), TODAY.minusDays(1), Set.of())).isEmpty();
            assertThat(service.findEvents(NYC, TODAY.plusDays(3), TODAY.plusDays(1), Set.of())).isEmpty();
            verifyNoInteractions(ticketmasterClient);
        }

        @Test
        @DisplayName("events that already started are removed")
        void pastRemoved() {
            Event past = event("past", "Brunch Jazz", at("2026-03-07T10:00:00"), true, Interest.MUSIC);
            Event exactlyNow = event("now", "Matinee", at("2026-03-07T10:30:00"), true, Interest.MUSIC);
            Event later = event("later", "Evening Show", at("2026-03-07T20:00:00"), true, Interest.MUSIC);
            stubSearch(past, exactlyNow, later);

            assertThat(service.findEvents(NYC, TODAY, TODAY, Set.of())).containsExactly(exactlyNow, later);
        }

        @Test
        @DisplayName("two interests sharing a classification: one Ticketmaster search")
        void sharedClassification() {
            covers(Interest.MUSIC, "Dance/Electronic");
            covers(Interest.NIGHTLIFE, "Dance/Electronic");
            Event rave = event("rave", "Warehouse Rave", at("2026-03-08T23:00:00"), true,
                    Interest.MUSIC, Interest.NIGHTLIFE);
            stubSearch(rave);

            List<Event> events = service.findEvents(NYC, TODAY, TODAY.plusDays(1),
                    Set.of(Interest.MUSIC, Interest.NIGHTLIFE));

            assertThat(events).containsExactly(rave);
            assertThat(capturedQuery().getClassificationName()).isEqualTo("Dance/Electronic");
        }

        @Test
        @DisplayName("interest filter: only events with a requested interest; uncovered interests skipped")
        void interestFilter() {
            covers(Interest.COMEDY, "Comedy");
            when(interestMappingService.hasCoverage(Interest.FOOD)).thenReturn(false);
            Event comedy = event("c", "Stand-up", at("2026-03-08T21:00:00"), true, Interest.COMEDY);
            Event music = event("m", "Comedy Rock Band", at("2026-03-08T21:00:00"), true, Interest.MUSIC);
            stubSearch(comedy, music);

            assertThat(service.findEvents(NYC, TODAY, TODAY.plusDays(1), Set.of(Interest.COMEDY, Interest.FOOD)))
                    .containsExactly(comedy);
            verify(interestMappingService, never()).classificationNamesFor(Interest.FOOD);
            verify(ticketmasterClient, times(1)).searchAllPages(any(), anyInt());
        }

        @Test
        @DisplayName("only uncovered interests: empty, no Ticketmaster call")
        void noCoverage() {
            when(interestMappingService.hasCoverage(Interest.FOOD)).thenReturn(false);

            assertThat(service.findEvents(NYC, TODAY, TODAY, Set.of(Interest.FOOD))).isEmpty();
            verifyNoInteractions(ticketmasterClient);
        }

        @Test
        @DisplayName("at most 6 searches")
        void maxSearches() {
            when(interestMappingService.hasCoverage(any())).thenReturn(true);
            when(interestMappingService.classificationNamesFor(Interest.THEATER))
                    .thenReturn(List.of("Theatre", "Musical", "Opera", "Dance"));
            when(interestMappingService.classificationNamesFor(Interest.ART))
                    .thenReturn(List.of("Fine Art", "Multimedia", "Cultural"));
            stubSearch();

            service.findEvents(NYC, TODAY, TODAY, Set.of(Interest.THEATER, Interest.ART));

            verify(ticketmasterClient, times(EventQueryServiceImpl.MAX_SEARCHES)).searchAllPages(any(), anyInt());
        }

        @Test
        @DisplayName("deduplicated, sorted by start then title, every event put in eventById")
        void dedupeSortAndCache() {
            Event late = event("late", "Zebra", at("2026-03-08T22:00:00"), true);
            Event earlyB = event("b", "beta", at("2026-03-08T19:00:00"), true);
            Event earlyA = event("a", "Alpha", at("2026-03-08T19:00:00"), false);
            Event dupOfA = event("a2", "alpha!", at("2026-03-08T19:00:00"), true);
            stubSearch(late, earlyB, earlyA, dupOfA);

            List<Event> events = service.findEvents(NYC, TODAY, TODAY.plusDays(1), Set.of());

            assertThat(events).containsExactly(dupOfA, earlyB, late);
            assertThat(service.findById(late.getId())).contains(late);
            verify(ticketmasterClient, never()).getEvent(any());
        }

        @Test
        @DisplayName("capped at 500 events")
        void capped() {
            Event[] many = IntStream.range(0, 600)
                    .mapToObj(i -> event("e" + i, "Show " + i, at("2026-03-08T12:00:00").plusMinutes(i), true))
                    .toArray(Event[]::new);
            stubSearch(many);

            List<Event> events = service.findEvents(NYC, TODAY, TODAY.plusDays(1), Set.of());

            assertThat(events).hasSize(EventQueryServiceImpl.MAX_EVENTS);
            assertThat(events.get(0)).isEqualTo(many[0]);
        }

        @Test
        @DisplayName("Ticketmaster down with a stale copy: stale copy returned")
        void staleFallback() {
            Event show = event("s", "Show", at("2026-03-08T20:00:00"), true);
            stubSearch(show);
            service.findEvents(NYC, TODAY, TODAY.plusDays(1), Set.of());
            cacheManager.getCache(CacheConfig.EVENT_SEARCH).clear();
            when(ticketmasterClient.searchAllPages(any(), anyInt())).thenThrow(unavailable());

            assertThat(service.findEvents(NYC, TODAY, TODAY.plusDays(1), Set.of())).containsExactly(show);
            verify(ticketmasterClient, times(2)).searchAllPages(any(), anyInt());
        }

        @Test
        @DisplayName("Ticketmaster down without a stale copy: rethrown")
        void noStaleCopy() {
            ServiceValidationException down = unavailable();
            when(ticketmasterClient.searchAllPages(any(), anyInt())).thenThrow(down);

            assertThatThrownBy(() -> service.findEvents(NYC, TODAY, TODAY.plusDays(1), Set.of())).isSameAs(down);
        }

        @Test
        @DisplayName("other errors are rethrown even with a stale copy")
        void otherErrorNotMasked() {
            stubSearch(event("s", "Show", at("2026-03-08T20:00:00"), true));
            service.findEvents(NYC, TODAY, TODAY.plusDays(1), Set.of());
            cacheManager.getCache(CacheConfig.EVENT_SEARCH).clear();
            ServiceValidationException other = new ServiceValidationException(ErrorCode.INTERNAL_ERROR,
                    kz.astyq.zimran.core.util.MessageCode.INTERNAL_ERROR);
            when(ticketmasterClient.searchAllPages(any(), anyInt())).thenThrow(other);

            assertThatThrownBy(() -> service.findEvents(NYC, TODAY, TODAY.plusDays(1), Set.of())).isSameAs(other);
        }

        @Test
        @DisplayName("identical second call is served from eventSearch")
        void cached() {
            stubSearch(event("s", "Show", at("2026-03-08T20:00:00"), true));

            service.findEvents(NYC, TODAY, TODAY.plusDays(1), Set.of());
            service.findEvents(NYC, TODAY, TODAY.plusDays(1), Set.of());

            verify(ticketmasterClient, times(1)).searchAllPages(any(), anyInt());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        private final EventId id = new EventId(EventSource.TICKETMASTER, "tm1");

        @Test
        @DisplayName("cache hit: no Ticketmaster call")
        void hit() {
            Event cached = event("tm1", "Cached", at("2026-03-08T20:00:00"), true);
            cacheManager.getCache(CacheConfig.EVENT_BY_ID).put(id, cached);

            assertThat(service.findById(id)).contains(cached);
            verifyNoInteractions(ticketmasterClient);
        }

        @Test
        @DisplayName("cache miss: fetched, normalized for the venue's supported city, then cached")
        void miss() {
            TmEvent tm = tmEventInCity("tm1", "New York");
            Event event = event("tm1", "Fetched", at("2026-03-08T20:00:00"), true);
            normalized.put("tm1", event);
            when(ticketmasterClient.getEvent("tm1")).thenReturn(Optional.of(tm));
            when(supportedCityService.findByName("New York")).thenReturn(Optional.of(NYC));

            assertThat(service.findById(id)).contains(event);
            assertThat(service.findById(id)).contains(event);

            verify(ticketmasterEventNormalizer).normalize(tm, NYC);
            verify(ticketmasterClient, times(1)).getEvent("tm1");
            verify(supportedCityService, never()).getDefault();
        }

        @Test
        @DisplayName("venue city not supported: normalized with the default city")
        void defaultCity() {
            TmEvent tm = tmEventInCity("tm1", "Brooklyn");
            normalized.put("tm1", event("tm1", "Fetched", at("2026-03-08T20:00:00"), true));
            when(ticketmasterClient.getEvent("tm1")).thenReturn(Optional.of(tm));
            when(supportedCityService.findByName("Brooklyn")).thenReturn(Optional.empty());
            when(supportedCityService.getDefault()).thenReturn(NYC);

            assertThat(service.findById(id)).isPresent();
            verify(ticketmasterEventNormalizer).normalize(tm, NYC);
        }

        @Test
        @DisplayName("404 from Ticketmaster: empty, nothing cached")
        void notFound() {
            when(ticketmasterClient.getEvent("tm1")).thenReturn(Optional.empty());

            assertThat(service.findById(id)).isEmpty();
            assertThat(service.findById(id)).isEmpty();
            verify(ticketmasterClient, times(2)).getEvent("tm1");
        }

        @Test
        @DisplayName("event the normalizer drops: empty")
        void dropped() {
            when(ticketmasterClient.getEvent("tm1")).thenReturn(Optional.of(tmEventInCity("tm1", null)));
            when(supportedCityService.findByName(null)).thenReturn(Optional.empty());
            when(supportedCityService.getDefault()).thenReturn(NYC);

            assertThat(service.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("null id: empty")
        void nullId() {
            assertThat(service.findById(null)).isEmpty();
            verifyNoInteractions(ticketmasterClient);
        }
    }

    private void stubSearch(Event... events) {
        events = events.clone();
        List<TmEvent> raw = new java.util.ArrayList<>();
        for (Event event : events) {
            normalized.put(event.getId().getSourceId(), event);
            raw.add(TmEvent.builder().id(event.getId().getSourceId()).build());
        }
        when(ticketmasterClient.searchAllPages(any(), anyInt())).thenReturn(raw);
    }

    private void covers(Interest interest, String... classificationNames) {
        when(interestMappingService.hasCoverage(interest)).thenReturn(true);
        when(interestMappingService.classificationNamesFor(interest)).thenReturn(List.of(classificationNames));
    }

    private TmSearchQuery capturedQuery() {
        ArgumentCaptor<TmSearchQuery> captor = ArgumentCaptor.forClass(TmSearchQuery.class);
        verify(ticketmasterClient).searchAllPages(captor.capture(), anyInt());
        return captor.getValue();
    }

    private static ServiceValidationException unavailable() {
        return new ServiceValidationException(ErrorCode.UPSTREAM_UNAVAILABLE, MessageCode.TICKETMASTER_UNAVAILABLE);
    }

    private static TmEvent tmEventInCity(String id, String cityName) {
        TmVenue venue = TmVenue.builder()
                .name("Venue")
                .city(cityName != null ? TmNamed.builder().name(cityName).build() : null)
                .build();
        return TmEvent.builder().id(id).embedded(TmEventEmbedded.builder().venues(List.of(venue)).build()).build();
    }

    /** New York local date-time. */
    private static OffsetDateTime at(String localDateTime) {
        return java.time.LocalDateTime.parse(localDateTime).atZone(NEW_YORK).toOffsetDateTime();
    }

    private static Event event(String id, String title, OffsetDateTime startAt, boolean priceKnown,
                               Interest... interests) {
        return Event.builder()
                .id(new EventId(EventSource.TICKETMASTER, id))
                .title(title)
                .sourceUrl("https://www.ticketmaster.com/event/" + id)
                .startAt(startAt)
                .timezone(NEW_YORK)
                .status(EventStatus.SCHEDULED)
                .category("Music")
                .interests(List.of(interests))
                .venue("Venue")
                .city("New York")
                .priceKnown(priceKnown)
                .priceMin(priceKnown ? BigDecimal.TEN : null)
                .priceMax(priceKnown ? BigDecimal.TEN : null)
                .currency(priceKnown ? "USD" : null)
                .build();
    }
}
