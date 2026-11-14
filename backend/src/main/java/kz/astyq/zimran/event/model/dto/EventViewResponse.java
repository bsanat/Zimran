package kz.astyq.zimran.event.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import kz.astyq.zimran.event.model.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One event, exactly the frontend's {@code EventData}. Every value comes from the event source; optional fields
 * are serialized as null, never omitted.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Event")
public class EventViewResponse {

    @Schema(description = "Stable public id, <source>:<sourceEventId>", example = "json:evt-123")
    private String id;
    @Schema(example = "json")
    private String source;
    @Schema(example = "evt-123")
    private String sourceEventId;
    private String title;
    @Schema(description = "Lowercase interest id", example = "music")
    private String category;
    @Schema(description = "Shared category slug of the primary interest (see GET /api/meta)", example = "music")
    private String categorySlug;
    @Schema(example = "New York")
    private String city;
    @Schema(nullable = true, description = "Start in the venue's timezone (ISO-8601 with offset)",
            example = "2026-11-20T20:00:00-05:00")
    private String startAt;
    @Schema(nullable = true, description = "Raw date text from the source, shown when startAt is null")
    private String dateText;
    @Schema(description = "IANA timezone of the venue", example = "America/New_York")
    private String timezone;
    @Schema(nullable = true)
    private String venue;
    @Schema(nullable = true)
    private String address;
    @Schema(nullable = true, description = "Plain text, at most 1000 characters")
    private String description;
    @Schema(nullable = true, description = "https only")
    private String imageUrl;
    @Schema(nullable = true, description = "Null when the price is unknown; 0 only when the source says free")
    private BigDecimal priceMin;
    @Schema(nullable = true)
    private BigDecimal priceMax;
    private boolean priceKnown;
    @Schema(nullable = true, description = "Raw price text from the source")
    private String priceText;
    @Schema(nullable = true, example = "USD")
    private String currency;
    @Schema(nullable = true, description = "Event page at the source, https only")
    private String sourceUrl;
    @Schema(allowableValues = {"scheduled", "cancelled", "postponed", "expired"})
    private EventStatus status;
    @JsonProperty("isMajor")
    @Schema(name = "isMajor")
    private boolean major;
    @JsonProperty("isDemo")
    @Schema(name = "isDemo", description = "Fictional sample event; show it as \"Sample / Demo\"")
    private boolean demo;
}
