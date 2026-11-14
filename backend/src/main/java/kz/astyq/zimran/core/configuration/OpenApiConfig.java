package kz.astyq.zimran.core.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.web.RequestScopeFilter;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /** Security scheme name for endpoints that need {@code X-Guest-Token}. */
    public static final String GUEST_TOKEN_SCHEME = "guestToken";
    /** Security scheme name for Pair Planning endpoints that need {@code X-Pair-Token}. */
    public static final String PAIR_TOKEN_SCHEME = "pairToken";
    /** Header carrying a Pair Planning participant token. */
    public static final String PAIR_TOKEN_HEADER = "X-Pair-Token";

    static {
        // @CurrentScope parameters are resolved from the token, not bound from the request.
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(RequestScope.class);
    }

    @Bean
    public OpenAPI zimranOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Zimran API")
                        .description("Zimran backend API")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes(GUEST_TOKEN_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(RequestScopeFilter.GUEST_TOKEN_HEADER)
                        .description("Token from POST /api/guest/session"))
                        .addSecuritySchemes(PAIR_TOKEN_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(PAIR_TOKEN_HEADER)
                                .description("participantToken from POST /api/pair/sessions or /api/pair/join")));
    }
}
