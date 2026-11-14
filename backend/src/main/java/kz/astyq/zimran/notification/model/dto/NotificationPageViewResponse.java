package kz.astyq.zimran.notification.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.astyq.zimran.event.model.dto.PaginationViewResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Body of {@code GET /api/notifications}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "One page of the notification center, newest first")
public class NotificationPageViewResponse {

    @Schema(description = "Unread notifications over all pages")
    private long unreadCount;
    private List<NotificationViewResponse> items;
    private PaginationViewResponse pagination;
}
