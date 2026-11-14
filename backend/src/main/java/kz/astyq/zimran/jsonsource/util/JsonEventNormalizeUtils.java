package kz.astyq.zimran.jsonsource.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Pure helpers that read loosely typed JSON values. Nothing here makes up a value: anything missing, blank or
 * malformed comes back as null.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonEventNormalizeUtils {

    private static final Pattern TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cc}\\p{Cf}&&[^\\n\\t]]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern PLAIN_NUMBER = Pattern.compile("\\d{1,9}(\\.\\d{1,4})?");
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._~-]{1,128}");
    private static final Pattern CURRENCY = Pattern.compile("[A-Za-z]{3}");

    /**
     * The first of {@code names} that is a non-blank string (or a number, as text) in {@code node}.
     */
    public static String text(JsonNode node, String... names) {
        if (node == null) {
            return null;
        }
        for (String name : names) {
            String value = scalar(node.get(name));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * A non-blank string or number value as stripped text; null for anything else.
     */
    public static String scalar(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isString()) {
            String text = value.stringValue().strip();
            return text.isEmpty() ? null : text;
        }
        if (value.isNumber()) {
            return value.numberValue().toString();
        }
        return null;
    }

    /**
     * Strings of a value that is a string or an array of strings (other elements ignored).
     */
    public static List<String> strings(JsonNode value) {
        List<String> result = new ArrayList<>();
        if (value == null || value.isNull()) {
            return result;
        }
        if (value.isArray()) {
            for (int i = 0; i < value.size(); i++) {
                String text = scalar(value.get(i));
                if (text != null) {
                    result.add(text);
                }
            }
            return result;
        }
        String text = scalar(value);
        if (text != null) {
            result.add(text);
        }
        return result;
    }

    /**
     * Single-line text: control characters removed, whitespace collapsed, at most {@code maxLength} code points.
     */
    public static String cleanText(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        String cleaned = WHITESPACE.matcher(CONTROL.matcher(text).replaceAll("")).replaceAll(" ").strip();
        return cleaned.isEmpty() ? null : truncate(cleaned, maxLength);
    }

    /**
     * Plain text from text that may hold HTML: tags removed, common entities decoded, whitespace collapsed,
     * at most {@code maxLength} code points (cut at a word boundary when possible).
     */
    public static String plainText(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        String withoutTags = TAGS.matcher(text).replaceAll(" ")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&apos;", "'").replace("&lt;", "<").replace("&gt;", ">");
        return cleanText(withoutTags, maxLength);
    }

    /**
     * {@code url} when it is an absolute https URL without credentials on the default port; else null.
     */
    public static String safeHttpsUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(url.strip());
            boolean safe = "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null && !uri.getHost().isBlank()
                    && uri.getRawUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443);
            return safe ? uri.toString() : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * A non-negative amount from a JSON number or a plain numeric string ({@code "25"}, {@code "25.50"});
     * null for anything else.
     */
    public static BigDecimal amount(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        BigDecimal amount = null;
        if (value.isNumber()) {
            Number number = value.numberValue();
            double asDouble = number.doubleValue();
            if (Double.isFinite(asDouble)) {
                amount = number instanceof BigDecimal decimal ? decimal : new BigDecimal(number.toString());
            }
        } else if (value.isString() && PLAIN_NUMBER.matcher(value.stringValue().strip()).matches()) {
            amount = new BigDecimal(value.stringValue().strip());
        }
        return amount != null && amount.signum() >= 0 ? amount.stripTrailingZeros().max(BigDecimal.ZERO) : null;
    }

    /**
     * The value of a JSON boolean, or of the strings {@code "true"}/{@code "false"}; null otherwise.
     */
    public static Boolean bool(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        String text = scalar(value);
        if ("true".equalsIgnoreCase(text)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(text)) {
            return Boolean.FALSE;
        }
        return null;
    }

    /**
     * Upper-case ISO 4217-shaped code, or null.
     */
    public static String currency(String value) {
        return value != null && CURRENCY.matcher(value.strip()).matches() ? value.strip().toUpperCase(Locale.ROOT) : null;
    }

    /**
     * {@code raw} when it is safe in a URL path segment, else a stable 24-hex-digit hash of it.
     */
    public static String urlSafeId(String raw) {
        return SAFE_ID.matcher(raw).matches() ? raw : hash(raw);
    }

    /**
     * First 24 hex digits of the SHA-256 of the parts joined by a separator.
     */
    public static String hash(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(String.join("\u001F", parts).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes).substring(0, 24);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String truncate(String text, int maxLength) {
        if (text.codePointCount(0, text.length()) <= maxLength) {
            return text;
        }
        String cut = text.substring(0, text.offsetByCodePoints(0, maxLength));
        int space = cut.lastIndexOf(' ');
        return (space > maxLength / 2 ? cut.substring(0, space) : cut).strip();
    }
}
