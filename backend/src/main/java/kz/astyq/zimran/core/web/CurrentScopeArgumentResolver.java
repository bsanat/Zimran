package kz.astyq.zimran.core.web;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.service.ScopeActivityRecorder;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.core.util.MessageCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link CurrentScope} parameters from the scope {@link RequestScopeFilter} verified. A missing or invalid
 * credential on a required scope fails with {@code 401 GUEST_TOKEN_INVALID}.
 */
@Slf4j
@RequiredArgsConstructor
public class CurrentScopeArgumentResolver implements HandlerMethodArgumentResolver {

    private final ObjectProvider<ScopeActivityRecorder> activityRecorders;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentScope.class)
                && RequestScope.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object scope = webRequest.getAttribute(RequestScopeFilter.SCOPE_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (scope instanceof RequestScope requestScope) {
            recordActivity(requestScope);
            return requestScope;
        }
        CurrentScope annotation = parameter.getParameterAnnotation(CurrentScope.class);
        if (annotation != null && !annotation.required()) {
            return null;
        }
        boolean credentialPresent = Boolean.TRUE.equals(webRequest.getAttribute(
                RequestScopeFilter.CREDENTIAL_PRESENT_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST));
        throw new ServiceValidationException(ErrorCode.GUEST_TOKEN_INVALID,
                credentialPresent ? MessageCode.GUEST_TOKEN_INVALID : MessageCode.GUEST_TOKEN_MISSING);
    }

    /**
     * Best effort: a failure here must not fail the request.
     */
    private void recordActivity(RequestScope scope) {
        activityRecorders.orderedStream().forEach(recorder -> {
            try {
                recorder.recordActivity(scope);
            } catch (RuntimeException e) {
                log.warn("Could not record activity of {}: {}", scope.getScopeId(), e.toString());
            }
        });
    }
}
