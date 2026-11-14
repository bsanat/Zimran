package kz.astyq.zimran.core.exception.handler;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import kz.astyq.zimran.core.exception.AiUnavailableException;
import kz.astyq.zimran.core.exception.NotFoundException;
import kz.astyq.zimran.core.exception.RateLimitedException;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.exception.UpstreamUnavailableException;
import kz.astyq.zimran.core.exception.ValidationException;
import kz.astyq.zimran.core.i18n.impl.MessageServiceImpl;
import kz.astyq.zimran.core.util.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(GlobalExceptionHandlerTest.ThrowingController.class)
@Import({GlobalExceptionHandlerTest.ThrowingController.class, MessageServiceImpl.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void notFoundException() {
        MvcTestResult result = mvc.get().uri("/api/test/not-found").exchange();

        assertError(result, 404, "NOT_FOUND", "The requested resource was not found");
        assertThat(result).bodyJson().extractingPath("$.error.details").asMap().isEmpty();
    }

    @Test
    void validationExceptionKeepsDetails() {
        MvcTestResult result = mvc.get().uri("/api/test/validation").exchange();

        assertError(result, 400, "VALIDATION_ERROR", "Request validation failed");
        assertThat(result).bodyJson().extractingPath("$.error.details.field").isEqualTo("city");
    }

    @Test
    void upstreamUnavailableException() throws Exception {
        MvcTestResult result = mvc.get().uri("/api/test/upstream").exchange();

        assertError(result, 503, "UPSTREAM_UNAVAILABLE", null);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("connection refused");
    }

    @Test
    void aiUnavailableException() {
        assertError(mvc.get().uri("/api/test/ai").exchange(), 503, "AI_UNAVAILABLE", null);
    }

    @Test
    void rateLimitedExceptionSetsRetryAfter() {
        MvcTestResult result = mvc.get().uri("/api/test/rate-limited").exchange();

        assertError(result, 429, "RATE_LIMITED", null);
        assertThat(result).headers().hasValue(HttpHeaders.RETRY_AFTER, "30");
        assertThat(result).bodyJson().extractingPath("$.error.details.retryAfterSeconds").isEqualTo(30);
    }

    @Test
    void legacyServiceValidationExceptionStillWorks() {
        assertError(mvc.get().uri("/api/test/legacy").exchange(), 400, "VALIDATION_ERROR", "Request validation failed");
    }

    @Test
    void unexpectedExceptionReturnsGenericMessage() throws Exception {
        MvcTestResult result = mvc.get().uri("/api/test/boom").exchange();

        assertError(result, 500, "INTERNAL_ERROR", "An unexpected error occurred");
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("secret internal detail")
                .doesNotContain("IllegalStateException")
                .doesNotContain("at kz.astyq");
    }

    @Test
    void invalidRequestBodyListsFieldErrors() {
        MvcTestResult result = mvc.post().uri("/api/test/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"city\":\"\",\"count\":9}")
                .exchange();

        assertError(result, 400, "VALIDATION_ERROR", "Request validation failed");
        assertThat(result).bodyJson().extractingPath("$.error.details.field").isEqualTo("city");
        assertThat(result).bodyJson().extractingPath("$.error.details.fields").asArray().hasSize(2);
        assertThat(result).bodyJson().extractingPath("$.error.details.fields[0].field").isEqualTo("city");
        assertThat(result).bodyJson().extractingPath("$.error.details.fields[0].message").asString().isNotBlank();
        assertThat(result).bodyJson().extractingPath("$.error.details.fields[1].field").isEqualTo("count");
    }

    @Test
    void invalidRequestParamListsFieldErrors() {
        MvcTestResult result = mvc.get().uri("/api/test/param?count=0").exchange();

        assertError(result, 400, "VALIDATION_ERROR", "Request validation failed");
        assertThat(result).bodyJson().extractingPath("$.error.details.field").isEqualTo("count");
        assertThat(result).bodyJson().extractingPath("$.error.details.fields[0].field").isEqualTo("count");
        assertThat(result).bodyJson().extractingPath("$.error.details.fields[0].message").asString().isNotBlank();
    }

    @Test
    void malformedJsonBody() {
        MvcTestResult result = mvc.post().uri("/api/test/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not json")
                .exchange();

        assertError(result, 400, "VALIDATION_ERROR", "Request body is missing or malformed");
    }

    @Test
    void typeMismatch() {
        MvcTestResult result = mvc.get().uri("/api/test/param?count=abc").exchange();

        assertError(result, 400, "VALIDATION_ERROR", "Parameter count has an invalid value");
        assertThat(result).bodyJson().extractingPath("$.error.details.field").isEqualTo("count");
    }

    @Test
    void missingRequestParameter() {
        MvcTestResult result = mvc.get().uri("/api/test/param").exchange();

        assertError(result, 400, "VALIDATION_ERROR", "Required parameter count is missing");
        assertThat(result).bodyJson().extractingPath("$.error.details.field").isEqualTo("count");
    }

    @Test
    void unknownPathIsNotFound() {
        assertError(mvc.get().uri("/api/test/does-not-exist").exchange(), 404, "NOT_FOUND",
                "The requested resource was not found");
    }

    @Test
    void unsupportedMethodIs405() {
        MvcTestResult result = mvc.delete().uri("/api/test/boom").exchange();

        assertError(result, 405, "VALIDATION_ERROR", "HTTP method DELETE is not supported for this endpoint");
        assertThat(result).headers().containsHeader(HttpHeaders.ALLOW);
    }

    @Test
    void wellFormedIncomingRequestIdIsUsedInBody() {
        MvcTestResult result = mvc.get().uri("/api/test/not-found").header("X-Request-Id", "client-req-12345").exchange();

        assertThat(result).headers().hasValue("X-Request-Id", "client-req-12345");
        assertThat(result).bodyJson().extractingPath("$.error.requestId").isEqualTo("client-req-12345");
    }

    @Test
    void corsPreflightForAllowedOrigin() {
        MvcTestResult result = mvc.options().uri("/api/test/boom")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).headers().hasValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000");
    }

    @Test
    void corsExposesRequestIdAndRetryAfter() {
        MvcTestResult result = mvc.get().uri("/api/test/rate-limited")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .exchange();

        assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS))
                .contains("X-Request-Id")
                .contains("Retry-After");
    }

    @Test
    void corsRejectsUnknownOrigin() {
        MvcTestResult result = mvc.options().uri("/api/test/boom")
                .header(HttpHeaders.ORIGIN, "https://evil.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange();

        assertThat(result).hasStatus(403);
        assertThat(result).doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }

    private static void assertError(MvcTestResult result, int status, String code, String message) {
        assertThat(result).hasStatus(status);
        assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON);

        String requestId = result.getResponse().getHeader("X-Request-Id");
        assertThat(requestId).isNotBlank();

        assertThat(result).bodyJson().extractingPath("$").asMap().containsOnlyKeys("error");
        assertThat(result).bodyJson().extractingPath("$.error").asMap()
                .containsOnlyKeys("code", "message", "details", "requestId");
        assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo(code);
        assertThat(result).bodyJson().extractingPath("$.error.requestId").isEqualTo(requestId);
        assertThat(result).bodyJson().extractingPath("$.error.details").asMap().isNotNull();
        if (message != null) {
            assertThat(result).bodyJson().extractingPath("$.error.message").isEqualTo(message);
        } else {
            assertThat(result).bodyJson().extractingPath("$.error.message").asString()
                    .isNotBlank()
                    .doesNotStartWith("error.");
        }
    }

    record Body(@NotBlank String city, @Min(1) @Max(5) int count) {
    }

    @RestController
    @RequestMapping("/api/test")
    static class ThrowingController {

        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException();
        }

        @GetMapping("/validation")
        void validation() {
            throw new ValidationException("error.validation", Map.of("field", "city"));
        }

        @GetMapping("/upstream")
        void upstream() {
            throw new UpstreamUnavailableException(new IOException("connection refused"));
        }

        @GetMapping("/ai")
        void ai() {
            throw new AiUnavailableException();
        }

        @GetMapping("/rate-limited")
        void rateLimited() {
            throw new RateLimitedException(30);
        }

        @GetMapping("/legacy")
        void legacy() {
            throw new ServiceValidationException(ErrorCode.VALIDATION_ERROR, "error.validation");
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("secret internal detail");
        }

        @PostMapping("/body")
        void body(@Valid @RequestBody Body body) {
        }

        @GetMapping("/param")
        void param(@RequestParam @Min(1) int count) {
        }
    }
}
