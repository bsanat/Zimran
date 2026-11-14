package kz.astyq.zimran.core.configuration;

import kz.astyq.zimran.core.model.AppSettings;
import kz.astyq.zimran.core.web.CaseInsensitiveEnumConverterFactory;
import kz.astyq.zimran.core.web.LanguageLocaleResolver;
import kz.astyq.zimran.core.web.RateLimitInterceptor;
import kz.astyq.zimran.core.web.RequestIdFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;
    private final WebProperties webProperties;
    private final AppSettings appSettings;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(corsProperties.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(RequestIdFilter.HEADER, HttpHeaders.RETRY_AFTER)
                .maxAge(3600);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new CaseInsensitiveEnumConverterFactory());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        WebProperties.RateLimit rateLimit = webProperties.getRateLimit();
        if (rateLimit.isEnabled() && !rateLimit.getPaths().isEmpty()) {
            registry.addInterceptor(new RateLimitInterceptor(rateLimit.getRequestsPerMinute(), Clock.systemUTC()))
                    .addPathPatterns(rateLimit.getPaths());
        }
    }

    /**
     * Bean name {@code localeResolver} replaces Spring's default (Accept-Language only).
     */
    @Bean
    public LocaleResolver localeResolver() {
        return new LanguageLocaleResolver(AppSettings.SUPPORTED_LOCALES, appSettings.getDefaultLocale());
    }
}
