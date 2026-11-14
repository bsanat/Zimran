package kz.astyq.zimran.core.exception.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Body of every error response. {@code details} is never null: it serializes as {@code {}} when empty.
 */
public record ErrorBody(String code, String message, Map<String, Object> details, String requestId) {

    public ErrorBody {
        details = details == null || details.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }
}
