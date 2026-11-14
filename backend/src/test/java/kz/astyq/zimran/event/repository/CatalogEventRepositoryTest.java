package kz.astyq.zimran.event.repository;

import jakarta.persistence.EntityManager;
import kz.astyq.zimran.core.MutableClock;
import kz.astyq.zimran.core.configuration.JpaAuditingConfig;
import kz.astyq.zimran.event.mapper.CatalogEventMapper;
import kz.astyq.zimran.event.mapper.CatalogEventMapperImpl;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventCriteria;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.domain.EventSchedule;
import kz.astyq.zimran.event.model.domain.TestEvents;
import kz.astyq.zimran.event.model.entity.CatalogEvent;
import kz.astyq.zimran.event.model.enums.CatalogEventStatus;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.event.service.impl.EventIngestionServiceImpl;
import kz.astyq.zimran.event.util.CatalogEventSpecifications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import static kz.astyq.zimran.event.model.domain.TestEvents.NYC;
import static kz.astyq.zimran.event.model.domain.TestEvents.event;
import static kz.astyq.zimran.event.model.domain.TestEvents.eventAt;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The catalog against a real Postgres (Flyway schema): upsert, expiry, converters and the SQL pre-filters.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, CatalogEventMapperImpl.class, EventIngestionServiceImpl.class,
        CatalogEventRepositoryTest.ClockConfig.class})
class CatalogEventRepositoryTest {

    /** 12:00 in New York on 2026-11-14. */
    private static final Instant NOW = Instant.parse("2026-11-14T17:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.1-bookworm");

    @TestConfiguration
    static class ClockConfig {
        @Bean
        MutableClock clock() {
            return new MutableClock(NOW);
        }
    }

    @Autowired
    private CatalogEventRepository repository;
    @Autowired
    private EventIngestionServiceImpl ingestionService;
    @Autowired
    private CatalogEventMapper mapper;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.set(NOW);
    }

    @Nested
    @DisplayName("upsert")
    class Upsert {

        @Test
        @DisplayName("second run updates the row, keeps firstSeenAt and reports nothing new")
        void insertThenUpdate() {
            Event first = eventAt("e1", TODAY.plusDays(2), "19:00").title("Old").build();
            assertThat(ingestionService.upsert(List.of(first))).containsExactly(first);
            flushAndClear();

            clock.advance(Duration.ofHours(1));
            assertThat(ingestionService.upsert(List.of(eventAt("e1", TODAY.plusDays(2), "19:00").title("New").build())))
                    .isEmpty();
            flushAndClear();

            CatalogEvent row = repository.findByProviderAndProviderEventId("json", "e1").orElseThrow();
            assertThat(repository.count()).isEqualTo(1);
            assertThat(row.getTitle()).isEqualTo("New");
            assertThat(row.getFirstSeenAt()).isEqualTo(NOW);
            assertThat(row.getFetchedAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
        }

        @Test
        @DisplayName("round trip keeps every value: schedule, interests, price, demo flag, venue timezone")
        void roundTrip() {
            EventSchedule schedule = EventSchedule.builder()
                    .firstDate(TODAY).lastDate(TODAY.plusDays(30)).dayOfWeek(DayOfWeek.SATURDAY).build();
            Event event = TestEvents.price(TestEvents.interests(event("rt"), Interest.ART, Interest.FAMILY), "12.50", "20")
                    .id(new EventId(EventSource.DEMO, "rt"))
                    .state("NY")
                    .schedule(schedule)
                    .dateText("Saturdays")
                    .startAt(TestEvents.at(TODAY, "10:00"))
                    .localDate(TODAY)
                    .venue("Hall")
                    .demo(true)
                    .build();
            ingestionService.upsert(List.of(event));
            flushAndClear();

            Event read = mapper.toDomain(repository.findByProviderAndProviderEventId("demo", "rt").orElseThrow());

            assertThat(read).usingRecursiveComparison().isEqualTo(event);
        }
    }

    @Nested
    @DisplayName("expireEndedBefore")
    class Expire {

        @Test
        @DisplayName("expires events whose last day in their timezone has passed; open and undated ones stay")
        void expires() {
            ingestionService.upsert(List.of(
                    eventAt("yesterday", TODAY.minusDays(1), "23:30").build(),
                    eventAt("today", TODAY, "08:00").build(),
                    event("undated").dateText("Soon").build(),
                    event("open").schedule(EventSchedule.builder().firstDate(TODAY.minusDays(9)).build()).build()));
            flushAndClear();

            assertThat(status("yesterday")).isEqualTo(CatalogEventStatus.EXPIRED);
            assertThat(status("today")).isEqualTo(CatalogEventStatus.ACTIVE);
            assertThat(status("undated")).isEqualTo(CatalogEventStatus.ACTIVE);
            assertThat(status("open")).isEqualTo(CatalogEventStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("CatalogEventSpecifications")
    class Specifications {

        @BeforeEach
        void seed() {
            LocalDate day = TODAY.plusDays(3);
            ingestionService.upsert(List.of(
                    TestEvents.price(eventAt("free", day, "10:00"), "0", "0").build(),
                    TestEvents.price(eventAt("cheap", day, "11:00"), "15", "40").build(),
                    eventAt("unknown", day, "12:00").build(),
                    TestEvents.interests(eventAt("food", day, "13:00"), Interest.FOOD).build(),
                    eventAt("later", day.plusDays(10), "13:00").build(),
                    eventAt("demo", day, "14:00").id(new EventId(EventSource.DEMO, "demo")).demo(true).build(),
                    eventAt("other-state", day, "15:00").state("TX").build(),
                    eventAt("boston", day, "16:00").city("Boston").build()));
            flushAndClear();
        }

        @Test
        @DisplayName("city + state, demo only when included")
        void cityAndDemo() {
            assertThat(ids(criteria().build(), false))
                    .containsExactlyInAnyOrder("free", "cheap", "unknown", "food", "later");
            assertThat(ids(criteria().build(), true)).contains("demo");
        }

        @Test
        @DisplayName("budgetMax and freeOnly never pass an unknown price")
        void prices() {
            assertThat(ids(criteria().budgetMax(new BigDecimal("20")).build(), false))
                    .containsExactlyInAnyOrder("free", "cheap");
            assertThat(ids(criteria().freeOnly(true).build(), false)).containsExactly("free");
        }

        @Test
        @DisplayName("category slug and widened date bounds")
        void categoryAndDates() {
            assertThat(ids(criteria().categorySlug(CategorySlug.FOOD_DRINKS).build(), false)).containsExactly("food");
            assertThat(ids(criteria().fromDate(TODAY).toDate(TODAY.plusDays(5)).build(), false))
                    .doesNotContain("later").contains("free");
        }

        @Test
        @DisplayName("providerRowsInCity ignores demo rows")
        void providerRows() {
            repository.deleteAll(repository.findAll(CatalogEventSpecifications.providerRowsInCity(NYC)));
            flushAndClear();

            assertThat(repository.exists(CatalogEventSpecifications.providerRowsInCity(NYC))).isFalse();
            assertThat(repository.exists(CatalogEventSpecifications.inCity(NYC, true))).isTrue();
        }

        private List<String> ids(EventCriteria criteria, boolean includeDemo) {
            return repository.findAll(CatalogEventSpecifications.matching(criteria, includeDemo)).stream()
                    .map(CatalogEvent::getProviderEventId).toList();
        }
    }

    @Test
    @DisplayName("interests column keeps declaration order and reads back as a set")
    void interestsColumn() {
        ingestionService.upsert(List.of(TestEvents.interests(event("i"), Interest.WELLNESS, Interest.MUSIC).build()));
        flushAndClear();

        Object raw = entityManager.createNativeQuery("select interests from events where provider_event_id = 'i'")
                .getSingleResult();
        assertThat(raw).isEqualTo("music,wellness");
        assertThat(repository.findByProviderAndProviderEventId("json", "i").orElseThrow().getInterests())
                .isEqualTo(EnumSet.of(Interest.MUSIC, Interest.WELLNESS));
    }

    private static EventCriteria.EventCriteriaBuilder criteria() {
        return EventCriteria.builder().city(NYC);
    }

    private CatalogEventStatus status(String providerEventId) {
        return repository.findByProviderAndProviderEventId("json", providerEventId).orElseThrow().getStatus();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
