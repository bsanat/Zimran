package kz.astyq.zimran.moderation.model.dto;

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
public class BlockViewResponse {

    @Schema(description = "Opaque reference of this block, for DELETE /api/blocks/{scopeRef}; never a scope id")
    private String scopeRef;
    private Instant createdAt;
}
