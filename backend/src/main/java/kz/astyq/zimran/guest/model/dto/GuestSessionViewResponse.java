package kz.astyq.zimran.guest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class GuestSessionViewResponse {

    @Schema(description = "Send as the X-Guest-Token header. Keep it secret.")
    private String guestToken;

    @Schema(example = "gst_3f2b8c1e1d1a4f7e9c550b5c2a7e9d10")
    private String scopeId;

    @Schema(example = "2026-11-21T12:00:00Z")
    private Instant expiresAt;
}
