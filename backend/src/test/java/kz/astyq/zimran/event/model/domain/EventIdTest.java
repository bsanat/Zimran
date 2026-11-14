package kz.astyq.zimran.event.model.domain;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.model.enums.EventSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventIdTest {

    @Nested
    @DisplayName("parse")
    class Parse {

        @Test
        @DisplayName("source:id, split at the first colon")
        void valid() {
            assertThat(EventId.parse("ticketmaster:vvG1zZ9KbGkM1a"))
                    .isEqualTo(new EventId(EventSource.TICKETMASTER, "vvG1zZ9KbGkM1a"));
            assertThat(EventId.parse("ticketmaster:a:b").getSourceId()).isEqualTo("a:b");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"eventbrite:1", "Ticketmaster:1", "ticketmaster", "ticketmaster:", "ticketmaster:  ", ":1"})
        @DisplayName("unknown source or malformed: NOT_FOUND")
        void invalid(String value) {
            assertThatThrownBy(() -> EventId.parse(value))
                    .isInstanceOfSatisfying(ServiceValidationException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("asString")
    class AsString {

        @Test
        @DisplayName("inverse of parse")
        void roundTrip() {
            EventId id = new EventId(EventSource.TICKETMASTER, "Z7r9jZ1A7");
            assertThat(id.asString()).isEqualTo("ticketmaster:Z7r9jZ1A7");
            assertThat(EventId.parse(id.asString())).isEqualTo(id);
        }
    }
}
