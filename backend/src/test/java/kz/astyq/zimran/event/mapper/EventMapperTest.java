package kz.astyq.zimran.event.mapper;

import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.dto.EventDetailsViewResponse;
import kz.astyq.zimran.event.model.dto.EventViewResponse;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.model.enums.Interest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventMapperTest {

    private final EventMapper mapper = Mappers.getMapper(EventMapper.class);

    private final Event full = Event.builder()
            .id(new EventId(EventSource.TICKETMASTER, "vvG1zZ9KbGkM1a"))
            .title("Jazz at the Village Vanguard")
            .sourceUrl("https://www.ticketmaster.com/event/vvG1zZ9KbGkM1a")
            .startAt(OffsetDateTime.parse("2026-11-20T20:00:00-05:00"))
            .timezone(ZoneId.of("America/New_York"))
            .status(EventStatus.SCHEDULED)
            .description("Two sets nightly.")
            .category("Music")
            .interests(List.of(Interest.MUSIC))
            .venue("Village Vanguard")
            .address("178 7th Ave S, New York, NY, 10014")
            .city("New York")
            .priceMin(new BigDecimal("35"))
            .priceMax(new BigDecimal("89.5"))
            .currency("USD")
            .priceKnown(true)
            .imageUrl("https://s1.ticketm.net/dam/a/123/jazz_16_9_1024.jpg")
            .build();

    @Nested
    @DisplayName("toDetailsViewResponse")
    class ToDetails {

        @Test
        @DisplayName("every field, id and timezone as strings")
        void allFields() {
            EventDetailsViewResponse response = mapper.toDetailsViewResponse(full);

            assertThat(response).usingRecursiveComparison()
                    .ignoringFields("id", "timezone")
                    .isEqualTo(full);
            assertThat(response.getId()).isEqualTo("ticketmaster:vvG1zZ9KbGkM1a");
            assertThat(response.getTimezone()).isEqualTo("America/New_York");
        }

        @Test
        @DisplayName("unknown price stays null, not 0")
        void unknownPrice() {
            Event noPrice = Event.builder()
                    .id(full.getId()).title(full.getTitle()).sourceUrl(full.getSourceUrl())
                    .startAt(full.getStartAt()).timezone(full.getTimezone()).status(full.getStatus())
                    .category(full.getCategory()).interests(List.of())
                    .build();

            EventDetailsViewResponse response = mapper.toDetailsViewResponse(noPrice);

            assertThat(response.getPriceMin()).isNull();
            assertThat(response.getPriceMax()).isNull();
            assertThat(response.getCurrency()).isNull();
            assertThat(response.isPriceKnown()).isFalse();
            assertThat(response.getVenue()).isNull();
        }
    }

    @Nested
    @DisplayName("toViewResponses")
    class ToViewResponses {

        @Test
        @DisplayName("list item without description and address; order kept")
        void list() {
            List<EventViewResponse> responses = mapper.toViewResponses(List.of(full));

            assertThat(responses).singleElement().satisfies(item -> {
                assertThat(item.getId()).isEqualTo("ticketmaster:vvG1zZ9KbGkM1a");
                assertThat(item.getStartAt()).isEqualTo(full.getStartAt());
                assertThat(item.getPriceMax()).isEqualByComparingTo("89.5");
                assertThat(item.getImageUrl()).isEqualTo(full.getImageUrl());
            });
            assertThat(mapper.toViewResponses(null)).isNull();
        }
    }
}
