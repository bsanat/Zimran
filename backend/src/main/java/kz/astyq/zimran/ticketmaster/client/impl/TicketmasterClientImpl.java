package kz.astyq.zimran.ticketmaster.client.impl;

import kz.astyq.zimran.core.configuration.TicketmasterProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.ticketmaster.client.TicketmasterClient;
import kz.astyq.zimran.ticketmaster.model.dto.TmEvent;
import kz.astyq.zimran.ticketmaster.model.dto.TmPage;
import kz.astyq.zimran.ticketmaster.model.dto.TmSearchQuery;
import kz.astyq.zimran.ticketmaster.model.dto.TmSearchResponse;
import kz.astyq.zimran.ticketmaster.util.MessageCode;
import kz.astyq.zimran.ticketmaster.util.RequestThrottle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <ul>
 *   <li>401/403: logged at ERROR, {@code UPSTREAM_UNAVAILABLE}, no retry.</li>
 *   <li>404 on {@link #getEvent}: {@link Optional#empty()}.</li>
 *   <li>429, 5xx, I/O errors and timeouts: retried twice (300 ms, 900 ms; a {@code Retry-After}
 *       header can lengthen the wait up to 2 s), then {@code UPSTREAM_UNAVAILABLE}.</li>
 *   <li>Every attempt takes a permit from the shared 5/s {@link RequestThrottle}; if none is available
 *       within 1 s the call fails fast with {@code UPSTREAM_UNAVAILABLE}.</li>
 * </ul>
 * The API key is never logged: logs show {@code apikey=***}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketmasterClientImpl implements TicketmasterClient {

    static final int MAX_PAGE_SIZE = 200;
    /** Discovery API rejects {@code page * size >= 1000}. */
    static final int DEEP_PAGING_LIMIT = 1000;
    static final List<Duration> RETRY_DELAYS = List.of(Duration.ofMillis(300), Duration.ofMillis(900));
    static final Duration RETRY_AFTER_CAP = Duration.ofSeconds(2);

    private static final String SEARCH_PATH = "/discovery/v2/events.json";
    private static final String EVENT_PATH = "/discovery/v2/events/{id}.json";
    private static final String API_KEY_PARAM = "apikey";
    private static final DateTimeFormatter TM_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
    private static final Pattern API_KEY_IN_TEXT = Pattern.compile("(?i)(apikey=)[^&\\s\"']*");
    private static final Pattern LINE_BREAKS = Pattern.compile("[\\r\\n]");

    private final TicketmasterProperties properties;
    private final RestClient ticketmasterRestClient;
    private final RequestThrottle ticketmasterRequestThrottle;

    @Override
    public TmSearchResponse searchEvents(TmSearchQuery query) {
        return get(SEARCH_PATH, Map.of(), searchParams(query), TmSearchResponse.class)
                .orElseThrow(() -> {
                    log.warn("Ticketmaster GET {} returned 404", SEARCH_PATH);
                    return unavailable();
                });
    }

    @Override
    public Optional<TmEvent> getEvent(String sourceEventId) {
        if (!StringUtils.hasText(sourceEventId)) {
            throw new IllegalArgumentException("sourceEventId must not be blank");
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("locale", "*");
        return get(EVENT_PATH, Map.of("id", sourceEventId), params, TmEvent.class);
    }

    @Override
    public List<TmEvent> searchAllPages(TmSearchQuery query, int maxPages) {
        int size = effectiveSize(query);
        int page = Math.max(0, query.getPage());
        List<TmEvent> events = new ArrayList<>();
        for (int fetched = 0; fetched < maxPages; fetched++) {
            TmSearchResponse response = searchEvents(query.toBuilder().page(page).size(size).build());
            List<TmEvent> pageEvents = response.events();
            events.addAll(pageEvents);
            if (pageEvents.isEmpty() || isLastPage(response.getPage(), page)
                    || (long) (page + 1) * size >= DEEP_PAGING_LIMIT) {
                break;
            }
            page++;
        }
        return events;
    }

    private static boolean isLastPage(TmPage meta, int requestedPage) {
        if (meta == null || meta.getTotalPages() == null) {
            return true;
        }
        int number = meta.getNumber() != null ? meta.getNumber() : requestedPage;
        return number + 1 >= meta.getTotalPages();
    }

    private Map<String, String> searchParams(TmSearchQuery q) {
        Map<String, String> params = new LinkedHashMap<>();
        putIfText(params, "city", q.getCity());
        putIfText(params, "stateCode", q.getStateCode());
        putIfText(params, "countryCode", q.getCountryCode());
        if (q.getStart() != null) {
            params.put("startDateTime", formatInstant(q.getStart()));
        }
        if (q.getEnd() != null) {
            params.put("endDateTime", formatInstant(q.getEnd()));
        }
        putIfText(params, "classificationName", q.getClassificationName());
        params.put("page", String.valueOf(Math.max(0, q.getPage())));
        params.put("size", String.valueOf(effectiveSize(q)));
        params.put("sort", "date,asc");
        params.put("locale", "*");
        return params;
    }

    static String formatInstant(Instant instant) {
        return TM_DATE_TIME.format(instant);
    }

    private int effectiveSize(TmSearchQuery q) {
        int size = q.getSize() > 0 ? q.getSize() : properties.getPageSize();
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private static void putIfText(Map<String, String> params, String name, String value) {
        if (StringUtils.hasText(value)) {
            params.put(name, value);
        }
    }

    // ------------------------------------------------------------------ HTTP + retry

    /**
     * GET with retry. Returns empty on 404; throws {@code UPSTREAM_UNAVAILABLE} otherwise.
     */
    private <T> Optional<T> get(String pathTemplate, Map<String, String> pathVars, Map<String, String> query,
                                Class<T> type) {
        if (!properties.hasApiKey()) {
            throw new ServiceValidationException(ErrorCode.UPSTREAM_UNAVAILABLE,
                    MessageCode.TICKETMASTER_NOT_CONFIGURED);
        }
        String path = expand(pathTemplate, pathVars);
        String loggableQuery = loggableQuery(query);
        AtomicInteger attempt = new AtomicInteger();
        AtomicReference<Duration> retryAfter = new AtomicReference<>();

        RetryTemplate retryTemplate = new RetryTemplate(new UpstreamRetryPolicy(retryAfter));
        try {
            return retryTemplate.execute(() ->
                    attempt(pathTemplate, pathVars, query, type, path, loggableQuery,
                            attempt.incrementAndGet(), retryAfter));
        } catch (RetryException e) {
            Throwable last = e.getCause();
            if (last instanceof ServiceValidationException serviceException) {
                throw serviceException;
            }
            log.warn("Ticketmaster GET {}?{} failed after {} attempt(s): {}",
                    path, loggableQuery, attempt.get(), last != null ? last.getMessage() : e.getMessage());
            throw unavailable().withCause(last);
        }
    }

    private <T> Optional<T> attempt(String pathTemplate, Map<String, String> pathVars, Map<String, String> query,
                                    Class<T> type, String path, String loggableQuery, int attempt,
                                    AtomicReference<Duration> retryAfter) {
        retryAfter.set(null);
        acquirePermit(path);
        long startNanos = System.nanoTime();
        try {
            return ticketmasterRestClient.get()
                    .uri(uriBuilder -> {
                        Map<String, String> vars = new LinkedHashMap<>(pathVars);
                        uriBuilder.path(pathTemplate);
                        query.keySet().forEach(name -> {
                            uriBuilder.queryParam(name, "{q." + name + "}");
                            vars.put("q." + name, query.get(name));
                        });
                        uriBuilder.queryParam(API_KEY_PARAM, "{q.apikey}");
                        vars.put("q.apikey", properties.getApiKey());
                        return uriBuilder.build(vars);
                    })
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        long ms = elapsedMs(startNanos);
                        log.debug("Ticketmaster GET {}?{} -> {} in {} ms (attempt {})",
                                path, loggableQuery, status, ms, attempt);
                        if (response.getStatusCode().is2xxSuccessful()) {
                            T body = response.bodyTo(type);
                            if (body == null) {
                                log.warn("Ticketmaster GET {} -> {} with empty body (attempt {})", path, status, attempt);
                                throw unavailable();
                            }
                            return Optional.of(body);
                        }
                        if (status == 404) {
                            return Optional.<T>empty();
                        }
                        if (status == 401 || status == 403) {
                            log.error("Ticketmaster rejected API key (GET {} -> {})", path, status);
                            throw unavailable();
                        }
                        if (status == 429 || response.getStatusCode().is5xxServerError()) {
                            Duration wait = parseRetryAfter(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
                            retryAfter.set(wait);
                            log.warn("Ticketmaster GET {}?{} -> {} in {} ms (attempt {}){}", path, loggableQuery,
                                    status, ms, attempt, wait != null ? ", Retry-After " + wait.toMillis() + " ms" : "");
                            throw new TransientUpstreamException("HTTP " + status);
                        }
                        log.warn("Ticketmaster GET {}?{} -> unexpected {} in {} ms (attempt {})",
                                path, loggableQuery, status, ms, attempt);
                        throw unavailable();
                    });
        } catch (ResourceAccessException e) {
            // I/O error or timeout. The message embeds the request URI (with the key): mask it.
            String reason = sanitize(rootMessage(e));
            log.warn("Ticketmaster GET {}?{} failed in {} ms (attempt {}): {}",
                    path, loggableQuery, elapsedMs(startNanos), attempt, reason);
            throw new TransientUpstreamException("I/O error: " + reason);
        } catch (RestClientException e) {
            // e.g. unreadable JSON: retrying will not help.
            log.warn("Ticketmaster GET {}?{} failed in {} ms (attempt {}): {}",
                    path, loggableQuery, elapsedMs(startNanos), attempt, sanitize(rootMessage(e)));
            throw unavailable();
        }
    }

    private static ServiceValidationException unavailable() {
        return new ServiceValidationException(ErrorCode.UPSTREAM_UNAVAILABLE, MessageCode.TICKETMASTER_UNAVAILABLE);
    }

    private void acquirePermit(String path) {
        try {
            if (!ticketmasterRequestThrottle.tryAcquire()) {
                log.warn("Ticketmaster GET {} throttled: no permit within {} ms", path,
                        ticketmasterRequestThrottle.getMaxWait().toMillis());
                throw unavailable();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable();
        }
    }

    /**
     * {@code Retry-After} as delta-seconds or HTTP-date; null if absent or unparseable.
     */
    static Duration parseRetryAfter(String header) {
        if (!StringUtils.hasText(header)) {
            return null;
        }
        String value = header.trim();
        try {
            long seconds = Long.parseLong(value);
            return seconds >= 0 ? Duration.ofSeconds(seconds) : null;
        } catch (NumberFormatException ignored) {
            // not delta-seconds, try HTTP-date
        }
        try {
            ZonedDateTime at = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
            Duration until = Duration.between(Instant.now(), at.toInstant());
            return until.isNegative() ? Duration.ZERO : until;
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static String expand(String template, Map<String, String> vars) {
        String result = template;
        for (Map.Entry<String, String> var : vars.entrySet()) {
            result = result.replace("{" + var.getKey() + "}", var.getValue());
        }
        return oneLine(result);
    }

    private static String loggableQuery(Map<String, String> query) {
        String params = query.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        return oneLine((params.isEmpty() ? "" : params + "&") + API_KEY_PARAM + "=***");
    }

    private String sanitize(String text) {
        if (text == null) {
            return null;
        }
        String masked = API_KEY_IN_TEXT.matcher(text).replaceAll("$1***");
        String key = properties.getApiKey();
        if (StringUtils.hasText(key)) {
            masked = masked.replace(key, "***");
        }
        return oneLine(masked);
    }

    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return root.getClass().getSimpleName() + (message != null ? ": " + message : "");
    }

    private static String oneLine(String text) {
        return LINE_BREAKS.matcher(text).replaceAll("_");
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    /**
     * Retryable failure (429, 5xx, I/O, timeout). Carries no cause so the request URI never leaks.
     */
    static final class TransientUpstreamException extends RuntimeException {
        TransientUpstreamException(String message) {
            super(message, null, false, false);
        }
    }

    /**
     * Retries {@link TransientUpstreamException} with the fixed delays in {@link #RETRY_DELAYS}; a
     * {@code Retry-After} from the last response can lengthen a delay up to {@link #RETRY_AFTER_CAP}.
     */
    private static final class UpstreamRetryPolicy implements RetryPolicy {

        private final AtomicReference<Duration> lastRetryAfter;

        UpstreamRetryPolicy(AtomicReference<Duration> lastRetryAfter) {
            this.lastRetryAfter = lastRetryAfter;
        }

        @Override
        public boolean shouldRetry(Throwable throwable) {
            return throwable instanceof TransientUpstreamException;
        }

        @Override
        public BackOff getBackOff() {
            return () -> new BackOffExecution() {
                private int retries;

                @Override
                public long nextBackOff() {
                    if (retries >= RETRY_DELAYS.size()) {
                        return STOP;
                    }
                    long delay = RETRY_DELAYS.get(retries++).toMillis();
                    Duration retryAfter = lastRetryAfter.get();
                    if (retryAfter != null) {
                        delay = Math.max(delay, Math.min(retryAfter.toMillis(), RETRY_AFTER_CAP.toMillis()));
                    }
                    return delay;
                }
            };
        }
    }
}
