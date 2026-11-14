package kz.astyq.zimran.core.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * Origins allowed to call {@code /api/**} (env {@code CORS_ALLOWED_ORIGINS}, comma-separated).
     */
    @NotEmpty
    private List<@NotBlank String> allowedOrigins = new ArrayList<>(List.of("http://localhost:3000"));
}
