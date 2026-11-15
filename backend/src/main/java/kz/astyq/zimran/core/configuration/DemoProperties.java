package kz.astyq.zimran.core.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Demo-only features ({@code app.demo}), e.g. "Try Premium Demo". Off unless enabled.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.demo")
public class DemoProperties {

    private boolean enabled = false;
    private boolean premiumEnabled = false;
}
