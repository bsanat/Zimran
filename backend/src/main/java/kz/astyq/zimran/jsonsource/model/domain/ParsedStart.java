package kz.astyq.zimran.jsonsource.model.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * What could be read from a source start value: the start with time and offset, only the day, or neither.
 */
@Value
@Builder
public class ParsedStart {

    public static final ParsedStart UNKNOWN = ParsedStart.builder().build();

    /** Null when the source gave no usable time. */
    OffsetDateTime startAt;
    /** Day in the event's timezone; null when the date is unknown. */
    LocalDate localDate;
}
