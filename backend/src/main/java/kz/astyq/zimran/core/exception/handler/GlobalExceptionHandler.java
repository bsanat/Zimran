package kz.astyq.zimran.core.exception.handler;

import kz.astyq.zimran.core.exception.ApiException;
import kz.astyq.zimran.core.exception.RateLimitedException;
import kz.astyq.zimran.core.exception.dto.ErrorBody;
import kz.astyq.zimran.core.exception.dto.ErrorResponse;
import kz.astyq.zimran.core.i18n.MessageService;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.core.web.RequestIdFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates every exception into the uniform error envelope
 * {@code {"error":{"code","message","details","requestId"}}}. Stack traces are logged, never returned.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    static final String KEY_MALFORMED_BODY = "error.validation.malformed-body";
    static final String KEY_TYPE_MISMATCH = "error.validation.type-mismatch";
    static final String KEY_MISSING_PARAMETER = "error.validation.missing-parameter";
    static final String KEY_METHOD_NOT_ALLOWED = "error.validation.method-not-allowed";

    private static final Comparator<Map<String, Object>> BY_FIELD_THEN_MESSAGE =
            Comparator.comparing((Map<String, Object> m) -> String.valueOf(m.get("field")))
                    .thenComparing(m -> String.valueOf(m.get("message")));

    private final MessageService messageService;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException e) {
        ErrorCode code = e.getCode();
        if (code.getStatus().is5xxServerError()) {
            log.warn("{}: {}", code, e.getMessageKey(), e.getCause());
        } else {
            log.debug("{}: {}", code, e.getMessageKey());
        }
        HttpHeaders headers = new HttpHeaders();
        if (e instanceof RateLimitedException rateLimited) {
            headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(rateLimited.getRetryAfterSeconds()));
        }
        return build(code.getStatus(), code, message(e.getMessageKey(), e.getArgs()), e.getDetails(), headers);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException e) {
        List<Map<String, Object>> fields = new ArrayList<>();
        e.getBindingResult().getFieldErrors().forEach(fe -> fields.add(fieldError(fe.getField(), fe)));
        fields.sort(BY_FIELD_THEN_MESSAGE);
        e.getBindingResult().getGlobalErrors().forEach(ge -> fields.add(fieldError(ge.getObjectName(), ge)));
        return validationFailed(fields);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException e) {
        List<Map<String, Object>> fields = new ArrayList<>();
        for (ParameterValidationResult result : e.getParameterValidationResults()) {
            if (result instanceof ParameterErrors errors) {
                errors.getFieldErrors().forEach(fe -> fields.add(fieldError(fe.getField(), fe)));
                errors.getGlobalErrors().forEach(ge ->
                        fields.add(fieldError(parameterName(result.getMethodParameter()), ge)));
            } else {
                String name = parameterName(result.getMethodParameter());
                result.getResolvableErrors().forEach(err -> fields.add(field(name, err.getDefaultMessage())));
            }
        }
        fields.sort(BY_FIELD_THEN_MESSAGE);
        e.getCrossParameterValidationResults().forEach(err -> fields.add(field("request", err.getDefaultMessage())));
        return validationFailed(fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        // The parser message may echo request content; keep it in debug logs only.
        log.debug("Unreadable request body: {}", e.getMessage());
        return validation(message(KEY_MALFORMED_BODY), Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return validation(message(KEY_TYPE_MISMATCH, e.getName()), Map.of("field", e.getName()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        return validation(message(KEY_MISSING_PARAMETER, e.getParameterName()),
                Map.of("field", e.getParameterName()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        ErrorCode code = ErrorCode.NOT_FOUND;
        return build(code.getStatus(), code, message(code.getDefaultMessageKey()), Map.of(), new HttpHeaders());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        HttpHeaders headers = new HttpHeaders();
        if (e.getSupportedHttpMethods() != null) {
            headers.setAllow(e.getSupportedHttpMethods());
        }
        return build(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.VALIDATION_ERROR,
                message(KEY_METHOD_NOT_ALLOWED, e.getMethod()), Map.of(), headers);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        // Other Spring MVC exceptions (415, 406, ResponseStatusException, ...) keep their status.
        if (e instanceof org.springframework.web.ErrorResponse springError
                && !springError.getStatusCode().is5xxServerError()) {
            HttpStatusCode status = springError.getStatusCode();
            ErrorCode code = ErrorCode.fromStatus(status);
            log.debug("{} -> {}", e.getClass().getSimpleName(), status);
            return build(status, code, message(code.getDefaultMessageKey()), Map.of(), springError.getHeaders());
        }
        log.error("Unexpected error", e);
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return build(code.getStatus(), code, message(code.getDefaultMessageKey()), Map.of(), new HttpHeaders());
    }

    private ResponseEntity<ErrorResponse> validationFailed(List<Map<String, Object>> fields) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (!fields.isEmpty()) {
            details.put("field", fields.getFirst().get("field"));
            details.put("fields", fields);
        }
        return validation(message(ErrorCode.VALIDATION_ERROR.getDefaultMessageKey()), details);
    }

    private ResponseEntity<ErrorResponse> validation(String message, Map<String, Object> details) {
        ErrorCode code = ErrorCode.VALIDATION_ERROR;
        return build(code.getStatus(), code, message, details, new HttpHeaders());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatusCode status, ErrorCode code, String message,
                                                Map<String, Object> details, HttpHeaders headers) {
        ErrorBody body = new ErrorBody(code.name(), message, details, RequestIdFilter.currentRequestId());
        return ResponseEntity.status(status)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(body));
    }

    private String message(String key, Object... args) {
        return messageService.getMessage(key, args);
    }

    private static Map<String, Object> fieldError(String field, ObjectError error) {
        return field(field, error.getDefaultMessage());
    }

    private static Map<String, Object> field(String field, String message) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("field", field);
        entry.put("message", message);
        return entry;
    }

    private static String parameterName(MethodParameter parameter) {
        String explicit = null;
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
        RequestHeader requestHeader = parameter.getParameterAnnotation(RequestHeader.class);
        if (requestParam != null) {
            explicit = firstText(requestParam.name(), requestParam.value());
        } else if (pathVariable != null) {
            explicit = firstText(pathVariable.name(), pathVariable.value());
        } else if (requestHeader != null) {
            explicit = firstText(requestHeader.name(), requestHeader.value());
        }
        if (explicit != null) {
            return explicit;
        }
        String name = parameter.getParameterName();
        return name != null ? name : "arg" + parameter.getParameterIndex();
    }

    private static String firstText(String a, String b) {
        return StringUtils.hasText(a) ? a : StringUtils.hasText(b) ? b : null;
    }
}
