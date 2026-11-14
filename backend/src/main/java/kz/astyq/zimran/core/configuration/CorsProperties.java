package kz.astyq.zimran.core.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * @param allowedOrigins origins allowed to call {@code /api/**} (env {@code CORS_ALLOWED_ORIGINS}, comma-separated)
 */
@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(@NotEmpty List<@NotBlank String> allowedOrigins) {
}
