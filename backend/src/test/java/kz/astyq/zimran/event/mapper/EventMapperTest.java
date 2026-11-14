package kz.astyq.zimran.event.mapper;

import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.TestEvents;
import kz.astyq.zimran.event.model.dto.EventViewResponse;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.model.enums.Interest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventMapperTest {

    private final EventMapper mapper = Mappers.getMapper(EventMapper.class);

    private final Event full = TestEvents.event("evt-1")
            .title("Jazz at the Village Vanguard")
            .category(Interest.MUSIC)
            .interests(EnumSet.of(Interest.MUSIC, Interest.NIGHTLIFE))
            .startAt(OffsetDateTime.parse("2026-11-20T20:00:00-05:00"))
            .localDate(LocalDate.of(2026, 11, 20))
            .dateText("Fri, Nov 20, 8 PM")
            .status(EventStatus.POSTPONED)
            .description("Two sets nightly.")
            .venue("Village Vanguard")
            .address("178 7th Ave S, New York, NY 10014")
            .priceMin(new BigDecimal("35"))
            .priceMax(new BigDecimal("89.5"))
            .currency("USD")
            .priceKnown(true)
            .priceText("$35-$89.50")
            .imageUrl("https://img.example.com/jazz.jpg")
            .sourceUrl("https://tickets.example.com/jazz")
            .major(true)
            .build();

    @Nested
    @DisplayName("toViewResponse")
    class ToViewResponse {

        @Test
        @DisplayName("every contract field; id, source, category, timezone and status as wire strings")
        void allFields() {
            EventViewResponse response = mapper.toViewResponse(full);

            assertThat(response.getId()).isEqualTo("json:evt-1");
            assertThat(response.getSource()).isEqualTo("json");
            assertThat(response.getSourceEventId()).isEqualTo("evt-1");
            assertThat(response.getTitle()).isEqualTo("Jazz at the Village Vanguard");
            assertThat(response.getCategory()).isEqualTo("music");
            assertThat(response.getCity()).isEqualTo("New York");
            assertThat(response.getStartAt()).isEqualTo("2026-11-20T20:00:00-05:00");
            assertThat(response.getDateText()).isEqualTo("Fri, Nov 20, 8 PM");
            assertThat(response.getTimezone()).isEqualTo("America/New_York");
            assertThat(response.getVenue()).isEqualTo("Village Vanguard");
            assertThat(response.getAddress()).isEqualTo("178 7th Ave S, New York, NY 10014");
            assertThat(response.getDescription()).isEqualTo("Two sets nightly.");
            assertThat(response.getImageUrl()).isEqualTo("https://img.example.com/jazz.jpg");
            assertThat(response.getPriceMin()).isEqualByComparingTo("35");
            assertThat(response.getPriceMax()).isEqualByComparingTo("89.5");
            assertThat(response.isPriceKnown()).isTrue();
            assertThat(response.getPriceText()).isEqualTo("$35-$89.50");
            assertThat(response.getCurrency()).isEqualTo("USD");
            assertThat(response.getSourceUrl()).isEqualTo("https://tickets.example.com/jazz");
            assertThat(response.getStatus()).isEqualTo(EventStatus.POSTPONED);
            assertThat(response.isMajor()).isTrue();
            assertThat(response.getCategorySlug()).isEqualTo("music");
            assertThat(response.isDemo()).isFalse();
        }

        @Test
        @DisplayName("categorySlug is the slug of the primary interest; demo flag is passed on")
        void slugAndDemo() {
            EventViewResponse response = mapper.toViewResponse(
                    TestEvents.interests(TestEvents.event("d"), Interest.FOOD).demo(true).build());

            assertThat(response.getCategory()).isEqualTo("food");
            assertThat(response.getCategorySlug()).isEqualTo("food_drinks");
            assertThat(response.isDemo()).isTrue();
        }

        @Test
        @DisplayName("optional fields stay null; nothing is filled in")
        void minimal() {
            EventViewResponse response = mapper.toViewResponse(TestEvents.event("evt-2").build());

            assertThat(response.getStartAt()).isNull();
            assertThat(response.getDateText()).isNull();
            assertThat(response.getVenue()).isNull();
            assertThat(response.getAddress()).isNull();
            assertThat(response.getDescription()).isNull();
            assertThat(response.getImageUrl()).isNull();
            assertThat(response.getSourceUrl()).isNull();
            assertThat(response.getPriceMin()).isNull();
            assertThat(response.getPriceMax()).isNull();
            assertThat(response.getCurrency()).isNull();
            assertThat(response.getPriceText()).isNull();
            assertThat(response.isPriceKnown()).isFalse();
            assertThat(response.isMajor()).isFalse();
            assertThat(response.getStatus()).isEqualTo(EventStatus.SCHEDULED);
        }

        @Test
        @DisplayName("startAt keeps its own offset and whole seconds")
        void startAtFormat() {
            Event utc = TestEvents.event("u").startAt(OffsetDateTime.of(2026, 7, 1, 23, 5, 0, 0, ZoneOffset.UTC)).build();
            Event summer = TestEvents.event("s").startAt(OffsetDateTime.parse("2026-07-01T19:05:00-04:00")).build();

            assertThat(mapper.toViewResponse(utc).getStartAt()).isEqualTo("2026-07-01T23:05:00Z");
            assertThat(mapper.toViewResponse(summer).getStartAt()).isEqualTo("2026-07-01T19:05:00-04:00");
        }
    }

    @Test
    @DisplayName("toViewResponses keeps the order")
    void list() {
        Event second = TestEvents.event("evt-2").build();

        assertThat(mapper.toViewResponses(List.of(full, second)))
                .extracting(EventViewResponse::getId)
                .containsExactly("json:evt-1", "json:evt-2");
    }
}
