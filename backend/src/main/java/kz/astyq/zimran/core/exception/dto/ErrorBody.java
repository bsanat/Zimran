package kz.astyq.zimran.core.exception.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Body of every error response. {@code details} is never null: it serializes as {@code {}} when empty.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorBody {

    private String code;
    private String message;
    private Map<String, Object> details;
    private String requestId;

    public Map<String, Object> getDetails() {
        return details != null ? details : Map.of();
    }
}
