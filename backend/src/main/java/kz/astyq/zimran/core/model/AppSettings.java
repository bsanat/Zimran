package kz.astyq.zimran.core.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "application")
public class AppSettings {

    /** Languages with a {@code messages_<lang>.properties} file. */
    public static final List<String> SUPPORTED_LOCALES = List.of("ru", "en", "kk");
    private static final String FALLBACK_LOCALE = "en";

    private String defaultLocale = FALLBACK_LOCALE;

    /**
     * Language of the current request ({@code ru}, {@code en} or {@code kk}), else the default.
     */
    public String getLocale() {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        if (SUPPORTED_LOCALES.contains(locale)) {
            return locale;
        }
        return defaultLocale != null ? defaultLocale : FALLBACK_LOCALE;
    }

}
