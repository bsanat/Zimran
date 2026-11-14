package kz.astyq.zimran.core.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import kz.astyq.zimran.core.configuration.WebProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Caps request bodies at {@code app.web.max-request-body-bytes}. A declared {@code Content-Length} over the limit
 * fails on the first read; a body without one fails as soon as the limit is crossed. Either way the reader gets a
 * {@link RequestBodyTooLargeException}, which the exception handler maps to 413.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class RequestBodyLimitFilter extends OncePerRequestFilter {

    private final WebProperties webProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(new LimitedRequest(request, webProperties.getMaxRequestBodyBytes()), response);
    }

    private static final class LimitedRequest extends HttpServletRequestWrapper {

        private final long limit;
        private ServletInputStream stream;

        private LimitedRequest(HttpServletRequest request, long limit) {
            super(request);
            this.limit = limit;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (stream == null) {
                stream = new LimitedInputStream(super.getInputStream(), limit, getContentLengthLong() > limit);
            }
            return stream;
        }
    }

    private static final class LimitedInputStream extends ServletInputStream {

        private final ServletInputStream delegate;
        private final long limit;
        private final boolean declaredTooLarge;
        private long count;

        private LimitedInputStream(ServletInputStream delegate, long limit, boolean declaredTooLarge) {
            this.delegate = delegate;
            this.limit = limit;
            this.declaredTooLarge = declaredTooLarge;
        }

        @Override
        public int read() throws IOException {
            check(0);
            int value = delegate.read();
            if (value >= 0) {
                check(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            check(0);
            int read = delegate.read(buffer, offset, length);
            if (read > 0) {
                check(read);
            }
            return read;
        }

        private void check(int read) throws RequestBodyTooLargeException {
            count += read;
            if (declaredTooLarge || count > limit) {
                throw new RequestBodyTooLargeException(limit);
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }
    }
}
