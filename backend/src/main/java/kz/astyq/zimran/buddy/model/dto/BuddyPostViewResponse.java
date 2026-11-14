package kz.astyq.zimran.buddy.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.astyq.zimran.buddy.model.enums.BuddyPostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A Buddy post as others see it. Never contains scope ids or contact data.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuddyPostViewResponse {

    private String id;
    private String eventId;
    private String displayName;
    private String shortBio;
    private String language;
    private String preferences;
    private Integer ageMin;
    private Integer ageMax;
    private BuddyPostStatus status;
    @Schema(description = "Whether the caller wrote this post (false without a guest session)")
    private boolean own;
    private Instant createdAt;
    @Schema(description = "When the event starts; the post expires then")
    private Instant expiresAt;
}
