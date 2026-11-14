package kz.astyq.zimran.core.exception.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Error envelope: {@code {"error":{"code":"...","message":"...","details":{...},"requestId":"..."}}}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private ErrorBody error;
}
