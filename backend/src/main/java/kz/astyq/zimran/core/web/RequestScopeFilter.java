package kz.astyq.zimran.core.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.service.RequestScopeVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Verifies the scope credential (e.g. {@code X-Guest-Token}) of every request and stores the resulting
 * {@link RequestScope} as a request attribute. It never rejects a request: endpoints that need a scope ask for it
 * with {@link CurrentScope}, all others ignore the header. Credentials are never logged.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class RequestScopeFilter extends OncePerRequestFilter {

    /** Header carrying the guest token. */
    public static final String GUEST_TOKEN_HEADER = "X-Guest-Token";
    public static final String SCOPE_ATTRIBUTE = RequestScopeFilter.class.getName() + ".scope";
    public static final String CREDENTIAL_PRESENT_ATTRIBUTE =
            RequestScopeFilter.class.getName() + ".credentialPresent";

    private final ObjectProvider<RequestScopeVerifier> verifiers;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean credentialPresent = false;
        for (RequestScopeVerifier verifier : verifiers.orderedStream().toList()) {
            String credential = request.getHeader(verifier.header());
            if (!StringUtils.hasText(credential)) {
                continue;
            }
            credentialPresent = true;
            Optional<RequestScope> scope = verifier.verify(credential.strip());
            if (scope.isPresent()) {
                request.setAttribute(SCOPE_ATTRIBUTE, scope.get());
                break;
            }
        }
        request.setAttribute(CREDENTIAL_PRESENT_ATTRIBUTE, credentialPresent);
        chain.doFilter(request, response);
    }
}
