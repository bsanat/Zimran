package kz.astyq.zimran.core.exception.handler;

import kz.astyq.zimran.core.i18n.impl.MessageServiceImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(TestErrorController.class)
@Import(MessageServiceImpl.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvcTester mvc;

    @Nested
    class ServiceValidationExceptionCases {

        @Test
        void notFound() {
            MvcTestResult result = mvc.get().uri("/api/test/not-found").exchange();

            assertError(result, 404, "NOT_FOUND", "The requested resource was not found");
            assertNoDetails(result);
        }

        @Test
        void keepsDetails() {
            MvcTestResult result = mvc.get().uri("/api/test/validation").exchange();

            assertError(result, 400, "VALIDATION_ERROR", "Request validation failed");
            assertThat(result).bodyJson().extractingPath("$.error.details").asMap().containsOnlyKeys("field");
            assertThat(result).bodyJson().extractingPath("$.error.details.field").isEqualTo("city");
        }

        @Test
        void resolvesMessageArguments() {
            MvcTestResult result = mvc.get().uri("/api/test/method-arg").exchange();

            assertError(result, 409, "CONFLICT", "HTTP method PATCH is not supported for this endpoint");
        }

        @Test
        void upstreamUnavailableHidesCause() throws Exception {
            MvcTestResult result = mvc.get().uri("/api/test/upstream").exchange();

            assertError(result, 503, "UPSTREAM_UNAVAILABLE",
                    "The event provider is temporarily unavailable, please try again later");
            assertNoDetails(result);
            assertThat(result.getResponse().getContentAsString()).doesNotContain("connection refused");
        }

        @Test
        void retryAfterHeader() {
            MvcTestResult result = mvc.get().uri("/api/test/rate-limited").exchange();

            assertError(result, 429, "RATE_LIMITED", "Request validation failed");
            assertThat(result).headers().hasValue(HttpHeaders.RETRY_AFTER, "30");
            assertThat(result).bodyJson().extractingPath("$.error.details.retryAfterSeconds").isEqualTo(30);
        }

        @Test
        void noRetryAfterHeaderByDefault() {
            assertThat(mvc.get().uri("/api/test/not-found").exchange())
                    .doesNotContainHeader(HttpHeaders.RETRY_AFTER);
        }

        @Test
        void unknownMessageCodeFallsBackToKey() {
            assertError(mvc.get().uri("/api/test/unknown-key").exchange(), 413, "PAYLOAD_TOO_LARGE", "no.such.key");
        }
    }

    @Nested
    class MethodArgumentNotValidExceptionCases {

        @Test
        void listsFieldErrors() {
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
    }

    @Nested
    class HandlerMethodValidationExceptionCases {

        @Test
        void listsParameterErrors() {
            MvcTestResult result = mvc.get().uri("/api/test/param?count=0").exchange();

            assertError(result, 400, "VALIDATION_ERROR", "Request validation failed");
            assertThat(result).bodyJson().extractingPath("$.error.details.field").isEqualTo("count");
            assertThat(result).bodyJson().extractingPath("$.error.details.fields[0].field").isEqualTo("count");
            assertThat(result).bodyJson().extractingPath("$.error.details.fields[0].message").asString().isNotBlank();
        }
    }

    @Nested
    class HttpMessageNotReadableExceptionCases {

        @Test
        void malformedJson() {
            MvcTestResult result = mvc.post().uri("/api/test/body")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{not json")
                    .exchange();

            assertError(result, 400, "VALIDATION_ERROR", "Request is malformed or has invalid parameters");
            assertNoDetails(result);
        }
    }

    @Nested
    class MethodArgumentTypeMismatchExceptionCases {

        @Test
        void namesTheField() {
            MvcTestResult result = mvc.get().uri("/api/test/param?count=abc").exchange();

            assertError(result, 400, "VALIDATION_ERROR", "Request is malformed or has invalid parameters");
            assertThat(result).bodyJson().extractingPath("$.error.details.field").isEqualTo("count");
            assertThat(result).bodyJson().extractingPath("$.error.details.fields[0].message").isEqualTo("Invalid value");
        }
    }

    @Nested
    class MissingServletRequestParameterExceptionCases {

        @Test
        void namesTheField() {
            MvcTestResult result = mvc.get().uri("/api/test/param").exchange();

            assertError(result, 400, "VALIDATION_ERROR", "Request is malformed or has invalid parameters");
            assertThat(result).bodyJson().extractingPath("$.error.details.field").isEqualTo("count");
        }
    }

    @Nested
    class NoResourceFoundExceptionCases {

        @Test
        void unknownPathIs404() {
            MvcTestResult result = mvc.get().uri("/api/test/does-not-exist").exchange();

            assertError(result, 404, "NOT_FOUND", "The requested resource was not found");
            assertNoDetails(result);
        }
    }

    @Nested
    class HttpRequestMethodNotSupportedExceptionCases {

        @Test
        void is405WithAllowHeader() {
            MvcTestResult result = mvc.delete().uri("/api/test/boom").exchange();

            assertError(result, 405, "VALIDATION_ERROR", "HTTP method DELETE is not supported for this endpoint");
            assertThat(result).headers().containsHeader(HttpHeaders.ALLOW);
            assertNoDetails(result);
        }
    }

    @Nested
    class UnexpectedExceptionCases {

        @Test
        void returnsGenericMessageWithoutInternals() throws Exception {
            MvcTestResult result = mvc.get().uri("/api/test/boom").exchange();

            assertError(result, 500, "INTERNAL_ERROR", "An unexpected error occurred");
            assertNoDetails(result);
            assertThat(result.getResponse().getContentAsString())
                    .doesNotContain("secret internal detail")
                    .doesNotContain("IllegalStateException")
                    .doesNotContain("at kz.astyq");
        }

        @Test
        void otherSpringClientErrorsKeepTheirStatus() {
            MvcTestResult result = mvc.post().uri("/api/test/body")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content("hello")
                    .exchange();

            assertError(result, 415, "VALIDATION_ERROR", "Request is malformed or has invalid parameters");
        }
    }

    @Nested
    class RequestIdAndCors {

        @Test
        void wellFormedIncomingRequestIdIsUsedInBody() {
            MvcTestResult result = mvc.get().uri("/api/test/not-found")
                    .header("X-Request-Id", "client-req-12345").exchange();

            assertThat(result).headers().hasValue("X-Request-Id", "client-req-12345");
            assertThat(result).bodyJson().extractingPath("$.error.requestId").isEqualTo("client-req-12345");
        }

        @Test
        void preflightForAllowedOrigin() {
            MvcTestResult result = mvc.options().uri("/api/test/boom")
                    .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).headers().hasValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000");
        }

        @Test
        void exposesRequestIdAndRetryAfter() {
            MvcTestResult result = mvc.get().uri("/api/test/rate-limited")
                    .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                    .exchange();

            assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS))
                    .contains("X-Request-Id")
                    .contains("Retry-After");
        }

        @Test
        void rejectsUnknownOrigin() {
            MvcTestResult result = mvc.options().uri("/api/test/boom")
                    .header(HttpHeaders.ORIGIN, "https://evil.example")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                    .exchange();

            assertThat(result).hasStatus(403);
            assertThat(result).doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
        }
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
        assertThat(result).bodyJson().extractingPath("$.error.message").isEqualTo(message);
        assertThat(result).bodyJson().extractingPath("$.error.requestId").isEqualTo(requestId);
        assertThat(result).bodyJson().extractingPath("$.error.details").asMap().isNotNull();
    }

    private static void assertNoDetails(MvcTestResult result) {
        assertThat(result).bodyJson().extractingPath("$.error.details").asMap().isEmpty();
    }
}
