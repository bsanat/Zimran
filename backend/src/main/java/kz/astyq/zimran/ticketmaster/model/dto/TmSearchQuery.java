package kz.astyq.zimran.ticketmaster.model.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Parameters of a Discovery API event search. Null strings and instants are omitted from the request.
 * {@code size <= 0} means "use {@code app.ticketmaster.page-size}"; sizes above 200 are capped.
 */
@Value
@Builder(toBuilder = true)
public class TmSearchQuery {

    String city;
    String stateCode;
    String countryCode;
    Instant start;
    Instant end;
    String classificationName;
    int page;
    int size;
}
