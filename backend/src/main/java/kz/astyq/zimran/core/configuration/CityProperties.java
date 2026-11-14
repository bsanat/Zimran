package kz.astyq.zimran.core.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Supported cities ({@code app.city}). The default city must be one of them.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.city")
public class CityProperties {

    @NotBlank
    private String defaultId;

    @NotEmpty
    private List<@Valid @NotNull CityDefinition> supported = new ArrayList<>();

    @AssertTrue(message = "app.city.default-id must be the id of a supported city")
    public boolean isDefaultSupported() {
        return defaultId == null || supported.stream().anyMatch(c -> c != null && defaultId.equals(c.getId()));
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CityDefinition {

        @NotBlank
        private String id;
        @NotBlank
        private String name;
        private String stateCode;
        private String countryCode;
        @NotNull
        private ZoneId zone;
    }
}
