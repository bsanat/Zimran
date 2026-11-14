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
 * A request to one of the caller's posts.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuddyIncomingRequestViewResponse {

    private String id;
    private String displayName;
    private String message;
    private BuddyRequestStatus status;
    private Instant createdAt;
    private Instant resolvedAt;
    @Schema(description = "Set when ACCEPTED: how to meet safely (i18n text)")
    private String safetyNotice;
}
