package kz.astyq.zimran.core.exception.dto;

import kz.astyq.zimran.core.util.ErrorCode;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionResponse {
    private String status;
    private ErrorCode code;
    private String message;
}
