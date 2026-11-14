package kz.astyq.zimran.core.exception.handler;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.core.util.MessageCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Test-only controller that triggers every error path of {@link GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/test")
class TestErrorController {

    @GetMapping("/not-found")
    void notFound() {
        throw new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.NOT_FOUND);
    }

    @GetMapping("/validation")
    void validation() {
        throw new ServiceValidationException(ErrorCode.VALIDATION_ERROR, MessageCode.VALIDATION_ERROR)
                .withDetail("field", "city");
    }

    @GetMapping("/method-arg")
    void withArgument() {
        throw new ServiceValidationException(ErrorCode.CONFLICT, MessageCode.METHOD_NOT_ALLOWED, "PATCH");
    }

    @GetMapping("/upstream")
    void upstream() {
        throw new ServiceValidationException(ErrorCode.UPSTREAM_UNAVAILABLE,
                kz.astyq.zimran.jsonsource.util.MessageCode.EVENTS_SOURCE_UNAVAILABLE)
                .withCause(new IOException("connection refused"));
    }

    @GetMapping("/rate-limited")
    void rateLimited() {
        throw new ServiceValidationException(ErrorCode.RATE_LIMITED, MessageCode.VALIDATION_ERROR)
                .withRetryAfter(30)
                .withDetail("retryAfterSeconds", 30);
    }

    @GetMapping("/unknown-key")
    void unknownKey() {
        throw new ServiceValidationException(ErrorCode.PAYLOAD_TOO_LARGE, "no.such.key");
    }

    @GetMapping("/boom")
    void boom() {
        throw new IllegalStateException("secret internal detail");
    }

    @PostMapping("/body")
    void body(@Valid @RequestBody TestBody body) {
    }

    @GetMapping("/param")
    void param(@RequestParam @Min(1) int count) {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    static class TestBody {
        @NotBlank
        private String city;
        @Min(1)
        @Max(5)
        private int count;
    }
}
