package kz.astyq.zimran.buddy.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.astyq.zimran.buddy.model.enums.BuddyRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A request the caller sent, with the post it went to.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuddyOutgoingRequestViewResponse {

    private String id;
    @Schema(description = "The name the caller sent")
    private String displayName;
    private String message;
    private BuddyRequestStatus status;
    private Instant createdAt;
    private Instant resolvedAt;
    private BuddyPostViewResponse post;
    @Schema(description = "Set when ACCEPTED: how to meet safely (i18n text)")
    private String safetyNotice;
}
