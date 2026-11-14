package kz.astyq.zimran.core.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd1234", "client-req-12345", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"})
    void reusesWellFormedIncomingId(String incoming) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcInChain = new AtomicReference<>();

        filter.doFilter(request(incoming), response, (req, res) -> mdcInChain.set(MDC.get(RequestIdFilter.MDC_KEY)));

        assertThat(mdcInChain.get()).isEqualTo(incoming);
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(incoming);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "short",                                   // < 8 chars
            "has space 123",                           // invalid char
            "inject\r\nX-Evil: 1",                     // header injection attempt
            "underscore_not_allowed",                  // invalid char
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0" // 65 chars
    })
    void replacesMissingOrMalformedIdWithUuid(String incoming) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcInChain = new AtomicReference<>();

        filter.doFilter(request(incoming), response, (req, res) -> mdcInChain.set(MDC.get(RequestIdFilter.MDC_KEY)));

        String generated = response.getHeader(RequestIdFilter.HEADER);
        assertThat(generated).isNotEqualTo(incoming);
        assertThat(UUID.fromString(generated).toString()).isEqualTo(generated);
        assertThat(mdcInChain.get()).isEqualTo(generated);
    }

    @Test
    void clearsMdcAfterRequest() throws Exception {
        filter.doFilter(request("abcd1234"), new MockHttpServletResponse(), (req, res) -> { });

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void clearsMdcWhenChainThrows() {
        FilterChain failing = (req, res) -> {
            throw new ServletException("boom");
        };

        assertThatThrownBy(() -> filter.doFilter(request("abcd1234"), new MockHttpServletResponse(), failing))
                .isInstanceOf(ServletException.class);
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    private static MockHttpServletRequest request(String requestId) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events");
        if (requestId != null) {
            request.addHeader(RequestIdFilter.HEADER, requestId);
        }
        return request;
    }
}
