package kz.astyq.zimran.core.web;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

import java.util.Locale;

/**
 * Binds query parameters to enums ignoring case, so the lowercase wire values ({@code music}, {@code evening})
 * are accepted. A blank value binds to null; an unknown value fails binding (400 with the parameter as field).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class CaseInsensitiveEnumConverterFactory implements ConverterFactory<String, Enum> {

    @Override
    public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
        return source -> {
            String value = source.strip();
            if (value.isEmpty()) {
                return null;
            }
            return (T) Enum.valueOf(targetType, value.toUpperCase(Locale.ROOT));
        };
    }
}
