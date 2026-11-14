package kz.astyq.zimran.event.service.impl;

import kz.astyq.zimran.event.mapper.CatalogEventMapper;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.entity.CatalogEvent;
import kz.astyq.zimran.event.model.enums.CatalogEventStatus;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.repository.CatalogEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static kz.astyq.zimran.event.model.domain.TestEvents.eventAt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventIngestionServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");
    private static final Instant EARLIER = Instant.parse("2026-11-01T08:00:00Z");
    private static final LocalDate DAY = LocalDate.of(2026, 11, 20);

    private final CatalogEventMapper mapper = Mappers.getMapper(CatalogEventMapper.class);

    @Mock
    private CatalogEventRepository repository;

    private EventIngestionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EventIngestionServiceImpl(repository, mapper, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Nested
    @DisplayName("upsert")
    class Upsert {

        @Test
        @DisplayName("inserts unknown events with firstSeenAt = fetchedAt = now and returns only them")
        @SuppressWarnings("unchecked")
        void insertsNew() {
            Event known = eventAt("known", DAY, "19:00").title("New title").build();
            Event fresh = eventAt("fresh", DAY, "20:00").build();
            CatalogEvent stored = stored(eventAt("known", DAY, "19:00").title("Old title").build());
            when(repository.findByProviderAndProviderEventIdIn(eq("json"), anyCollection())).thenReturn(List.of(stored));

            List<Event> inserted = service.upsert(List.of(known, fresh));

            assertThat(inserted).containsExactly(fresh);
            ArgumentCaptor<List<CatalogEvent>> saved = ArgumentCaptor.forClass(List.class);
            verify(repository).saveAll(saved.capture());
            assertThat(saved.getValue()).singleElement().satisfies(row -> {
                assertThat(row.getProviderEventId()).isEqualTo("fresh");
                assertThat(row.getFirstSeenAt()).isEqualTo(NOW);
                assertThat(row.getFetchedAt()).isEqualTo(NOW);
                assertThat(row.getStatus()).isEqualTo(CatalogEventStatus.ACTIVE);
            });
        }

        @Test
        @DisplayName("updates a known event in place and never touches its firstSeenAt")
        void updatesKnown() {
            CatalogEvent stored = stored(eventAt("known", DAY, "19:00").title("Old title").build());
            when(repository.findByProviderAndProviderEventIdIn(eq("json"), anyCollection())).thenReturn(List.of(stored));

            List<Event> inserted = service.upsert(List.of(eventAt("known", DAY, "19:00").title("New title")
                    .status(EventStatus.CANCELLED).build()));

            assertThat(inserted).isEmpty();
            assertThat(stored.getTitle()).isEqualTo("New title");
            assertThat(stored.getStatus()).isEqualTo(CatalogEventStatus.CANCELLED);
            assertThat(stored.getFirstSeenAt()).isEqualTo(EARLIER);
            assertThat(stored.getFetchedAt()).isEqualTo(NOW);
            assertThat(stored.getProvider()).isEqualTo("json");
        }

        @Test
        @DisplayName("locks before reading and expires past events after writing")
        void lockThenExpire() {
            service.upsert(List.of(eventAt("a", DAY, "19:00").build()));

            InOrder order = inOrder(repository);
            order.verify(repository).lockForUpdate(EventIngestionServiceImpl.LOCK_KEY);
            order.verify(repository).findByProviderAndProviderEventIdIn(eq("json"), anyCollection());
            order.verify(repository).saveAll(any());
            order.verify(repository).expireEndedBefore(NOW);
        }

        @Test
        @DisplayName("a duplicate id in the input is written once (the first wins)")
        void duplicateInput() {
            Event first = eventAt("dup", DAY, "19:00").title("First").build();
            Event second = eventAt("dup", DAY, "19:00").title("Second").build();

            assertThat(service.upsert(List.of(first, second))).containsExactly(first);
        }

        @Test
        @DisplayName("nothing to write: no lock, no query")
        void empty() {
            assertThat(service.upsert(List.of())).isEmpty();
            assertThat(service.upsert(null)).isEmpty();
            verifyNoInteractions(repository);
        }
    }

    @Test
    @DisplayName("expirePastEvents: expires against the clock")
    void expirePastEvents() {
        when(repository.expireEndedBefore(NOW)).thenReturn(3);

        assertThat(service.expirePastEvents()).isEqualTo(3);
        verify(repository, never()).lockForUpdate(anyLong());
    }

    private CatalogEvent stored(Event event) {
        CatalogEvent row = mapper.toEntity(event);
        row.setId(7L);
        row.setFirstSeenAt(EARLIER);
        row.setFetchedAt(EARLIER);
        return row;
    }
}
