package kz.astyq.zimran.core.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Public address of the frontend ({@code app.frontend-url}), used to build links such as Pair invites.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app")
public class FrontendProperties {

    /**
     * Base URL without a trailing slash (env {@code FRONTEND_URL}).
     */
    @NotBlank
    @Pattern(regexp = "https?://\\S+", message = "app.frontend-url must be an http(s) URL")
    private String frontendUrl = "http://localhost:3000";

    /**
     * {@link #frontendUrl} without trailing slashes.
     */
    public String baseUrl() {
        return frontendUrl.replaceAll("/+$", "");
    }
}
