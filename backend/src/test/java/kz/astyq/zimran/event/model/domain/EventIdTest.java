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
            assertThat(EventId.parse("json:evt-123")).isEqualTo(new EventId(EventSource.JSON, "evt-123"));
            assertThat(EventId.parse("json:a:b").getSourceId()).isEqualTo("a:b");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"ticketmaster:1", "JSON:1", "json", "json:", "json:  ", ":1"})
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
            EventId id = new EventId(EventSource.JSON, "Z7r9jZ1A7");
            assertThat(id.asString()).isEqualTo("json:Z7r9jZ1A7");
            assertThat(EventId.parse(id.asString())).isEqualTo(id);
        }
    }
}
