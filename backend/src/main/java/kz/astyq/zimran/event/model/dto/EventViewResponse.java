package kz.astyq.zimran.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.model.enums.Interest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * One event in a list. Every value comes from the provider; optional fields are serialized as null.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Event in a list")
public class EventViewResponse {

    @Schema(example = "ticketmaster:vvG1zZ9KbGkM1a")
    private String id;
    private String title;
    @Schema(description = "Provider segment, e.g. Music; Other when unknown")
    private String category;
    private List<Interest> interests;
    @Schema(description = "Start in the venue's timezone (ISO-8601 with offset)", example = "2026-11-20T20:00:00-05:00")
    private OffsetDateTime startAt;
    @Schema(description = "IANA timezone of the venue", example = "America/New_York")
    private String timezone;
    private EventStatus status;
    @Schema(nullable = true)
    private String venue;
    @Schema(nullable = true)
    private String city;
    @Schema(nullable = true, description = "Null when the price is unknown (never 0 unless the provider says 0)")
    private BigDecimal priceMin;
    @Schema(nullable = true)
    private BigDecimal priceMax;
    @Schema(nullable = true, example = "USD")
    private String currency;
    private boolean priceKnown;
    @Schema(nullable = true)
    private String imageUrl;
    @Schema(description = "Provider page where tickets are sold")
    private String sourceUrl;
}
