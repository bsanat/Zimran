package kz.astyq.zimran.core.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Language of the response: the frontend's {@code outly-lang} cookie when it names a supported language, else the
 * best supported {@code Accept-Language} entry, else the default. Only the language part is kept.
 */
public class LanguageLocaleResolver implements LocaleResolver {

    public static final String COOKIE = "outly-lang";

    private final List<String> supported;
    private final Locale defaultLocale;

    public LanguageLocaleResolver(List<String> supported, String defaultLanguage) {
        this.supported = List.copyOf(supported);
        this.defaultLocale = Locale.forLanguageTag(defaultLanguage);
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String fromCookie = cookieLanguage(request);
        if (fromCookie != null) {
            return Locale.forLanguageTag(fromCookie);
        }
        String header = request.getHeader("Accept-Language");
        if (StringUtils.hasText(header)) {
            try {
                for (Locale.LanguageRange range : Locale.LanguageRange.parse(header)) {
                    String language = range.getRange().split("-", 2)[0].toLowerCase(Locale.ROOT);
                    if (supported.contains(language)) {
                        return Locale.forLanguageTag(language);
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // A malformed header falls back to the default language.
            }
        }
        return defaultLocale;
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException("The language is chosen by the outly-lang cookie");
    }

    private String cookieLanguage(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE.equals(cookie.getName()) && cookie.getValue() != null) {
                String value = cookie.getValue().strip().toLowerCase(Locale.ROOT);
                if (supported.contains(value)) {
                    return value;
                }
            }
        }
        return null;
    }
}
