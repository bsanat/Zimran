package kz.astyq.zimran.core.exception.handler;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.exception.dto.ErrorBody;
import kz.astyq.zimran.core.exception.dto.ErrorResponse;
import kz.astyq.zimran.core.i18n.MessageService;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.core.util.MessageCode;
import kz.astyq.zimran.core.web.RequestBodyTooLargeException;
import kz.astyq.zimran.core.web.RequestIdFilter;
import tools.jackson.core.JacksonException;
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
import org.springframework.validation.FieldError;
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

    private static final Comparator<Map<String, Object>> BY_FIELD_THEN_MESSAGE =
            Comparator.comparing((Map<String, Object> m) -> String.valueOf(m.get("field")))
                    .thenComparing(m -> String.valueOf(m.get("message")));

    private final MessageService messageService;

    @ExceptionHandler(ServiceValidationException.class)
    public ResponseEntity<ErrorResponse> handleService(ServiceValidationException e) {
        ErrorCode code = e.getErrorCode();
        if (code.getStatus().is5xxServerError()) {
            log.warn("{}: {}", code, e.getMessageCode(), e.getCause());
        } else {
            log.debug("{}: {}", code, e.getMessageCode());
        }
        HttpHeaders headers = new HttpHeaders();
        if (e.getRetryAfterSeconds() != null) {
            headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()));
        }
        return build(code.getStatus(), code, message(e.getMessageCode(), e.getArguments()), e.getDetails(), headers);
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
        if (causedBy(e, RequestBodyTooLargeException.class)) {
            ErrorCode code = ErrorCode.PAYLOAD_TOO_LARGE;
            return build(code.getStatus(), code, message(MessageCode.PAYLOAD_TOO_LARGE), Map.of(), new HttpHeaders());
        }
        String field = jsonField(e);
        return malformed(field != null ? singleField(field) : Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return malformed(singleField(e.getName()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        return malformed(singleField(e.getParameterName()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        ErrorCode code = ErrorCode.NOT_FOUND;
        return build(code.getStatus(), code, message(MessageCode.NOT_FOUND), Map.of(), new HttpHeaders());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        HttpHeaders headers = new HttpHeaders();
        if (e.getSupportedHttpMethods() != null) {
            headers.setAllow(e.getSupportedHttpMethods());
        }
        return build(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.VALIDATION_ERROR,
                message(MessageCode.METHOD_NOT_ALLOWED, e.getMethod()), Map.of(), headers);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        // The body limit can surface as a raw IOException when it is hit before a converter wraps it.
        if (causedBy(e, RequestBodyTooLargeException.class)) {
            ErrorCode code = ErrorCode.PAYLOAD_TOO_LARGE;
            return build(code.getStatus(), code, message(MessageCode.PAYLOAD_TOO_LARGE), Map.of(), new HttpHeaders());
        }
        // Other Spring MVC client errors (415, 406, ...) keep their 4xx status instead of becoming 500.
        if (e instanceof org.springframework.web.ErrorResponse springError
                && springError.getStatusCode().is4xxClientError()) {
            HttpStatusCode status = springError.getStatusCode();
            log.debug("{} -> {}", e.getClass().getSimpleName(), status);
            return build(status, ErrorCode.fromStatus(status), message(MessageCode.MALFORMED_REQUEST), Map.of(),
                    springError.getHeaders());
        }
        log.error("Unexpected error", e);
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return build(code.getStatus(), code, message(MessageCode.INTERNAL_ERROR), Map.of(), new HttpHeaders());
    }

    private ResponseEntity<ErrorResponse> validationFailed(List<Map<String, Object>> fields) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (!fields.isEmpty()) {
            details.put("field", fields.getFirst().get("field"));
            details.put("fields", fields);
        }
        ErrorCode code = ErrorCode.VALIDATION_ERROR;
        return build(code.getStatus(), code, message(MessageCode.VALIDATION_ERROR), details, new HttpHeaders());
    }

    private ResponseEntity<ErrorResponse> malformed(Map<String, Object> details) {
        ErrorCode code = ErrorCode.VALIDATION_ERROR;
        return build(code.getStatus(), code, message(MessageCode.MALFORMED_REQUEST), details, new HttpHeaders());
    }

    private Map<String, Object> singleField(String name) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("field", name);
        details.put("fields", List.of(field(name, null)));
        return details;
    }

    private ResponseEntity<ErrorResponse> build(HttpStatusCode status, ErrorCode code, String message,
                                                Map<String, Object> details, HttpHeaders headers) {
        ErrorBody body = ErrorBody.builder()
                .code(code.name())
                .message(message)
                .details(details == null || details.isEmpty() ? Map.of() : details)
                .requestId(RequestIdFilter.currentRequestId())
                .build();
        return ResponseEntity.status(status)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse.builder().error(body).build());
    }

    private String message(String code, Object... args) {
        return messageService.getMessage(code, args);
    }

    /**
     * A conversion failure (e.g. an unknown enum value) gets the generic text: its default message names
     * Java types and echoes the input.
     */
    private Map<String, Object> fieldError(String field, ObjectError error) {
        boolean bindingFailure = error instanceof FieldError fieldError && fieldError.isBindingFailure();
        return field(field, bindingFailure ? null : error.getDefaultMessage());
    }

    /**
     * One {@code {field, message}} entry; falls back to the generic "invalid value" text.
     */
    private Map<String, Object> field(String field, String message) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("field", field);
        entry.put("message", StringUtils.hasText(message) ? message : message(MessageCode.FIELD_INVALID));
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

    private static boolean causedBy(Throwable error, Class<? extends Throwable> type) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
            if (current.getCause() == current) {
                return false;
            }
        }
        return false;
    }

    /**
     * Top-level JSON property a body parse error happened in (e.g. {@code interests} for an unknown interest at
     * {@code interests[0]}), or null when Jackson did not record a path.
     */
    private static String jsonField(Throwable error) {
        for (Throwable current = error; current != null && current.getCause() != current; current = current.getCause()) {
            if (current instanceof JacksonException jackson) {
                return jackson.getPath().stream()
                        .map(JacksonException.Reference::getPropertyName)
                        .filter(StringUtils::hasText)
                        .findFirst()
                        .orElse(null);
            }
        }
        return null;
    }

    private static String firstText(String a, String b) {
        return StringUtils.hasText(a) ? a : StringUtils.hasText(b) ? b : null;
    }
}
