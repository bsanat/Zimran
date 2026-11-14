package kz.astyq.zimran.core.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the caller's {@link kz.astyq.zimran.core.model.domain.RequestScope} into a controller parameter.
 * When {@link #required()} and there is no valid scope the request fails with {@code 401 GUEST_TOKEN_INVALID};
 * otherwise the parameter is {@code null}.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentScope {

    boolean required() default true;
}
