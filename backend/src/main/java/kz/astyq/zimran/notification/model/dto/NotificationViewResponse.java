package kz.astyq.zimran.notification.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.astyq.zimran.event.model.dto.EventViewResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "One notification")
public class NotificationViewResponse {

    @Schema(example = "notif_3q2-7wEj0rP1dXo4m5Yt2A")
    private String id;
    @Schema(nullable = true, description = "Public event id; null when the event left the catalog",
            example = "json:evt-123")
    private String eventId;
    @Schema(description = "Localized title")
    private String title;
    @Schema(example = "music")
    private String categorySlug;
    private Instant createdAt;
    @Schema(nullable = true)
    private Instant readAt;
    @Schema(nullable = true, description = "The event as the catalog has it now; null when it left the catalog")
    private EventViewResponse event;
}
