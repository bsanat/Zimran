package kz.astyq.zimran.ticketmaster.util;

import kz.astyq.zimran.ticketmaster.model.dto.TmImage;
import kz.astyq.zimran.ticketmaster.model.dto.TmPriceRange;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Pure helpers for normalizing Ticketmaster data. No I/O, no Spring beans.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TicketmasterNormalizeUtils {

    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    public static final String PREFERRED_IMAGE_RATIO = "16_9";

    private static final Pattern SCRIPT_OR_STYLE = Pattern.compile("(?is)<(script|style)\\b[^>]*>.*?</\\1\\s*>");
    private static final Pattern HTML_COMMENT = Pattern.compile("(?s)<!--.*?-->");
    /** Tags that separate words; any other tag is removed without leaving a space. */
    private static final Pattern BLOCK_TAG = Pattern.compile(
            "(?i)</?(br|p|div|li|ul|ol|h[1-6]|tr|td|th|table|section|article|blockquote|hr)\\b[^>]*>");
    private static final Pattern TAG = Pattern.compile("<[^>]*>");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s\\u00A0]+");

    /**
     * Text without HTML tags, with common entities decoded and whitespace collapsed; null if blank.
     */
    public static String stripHtml(String raw) {
        if (raw == null) {
            return null;
        }
        String text = SCRIPT_OR_STYLE.matcher(raw).replaceAll(" ");
        text = HTML_COMMENT.matcher(text).replaceAll(" ");
        text = BLOCK_TAG.matcher(text).replaceAll(" ");
        text = TAG.matcher(text).replaceAll("");
        text = decodeEntities(text);
        text = WHITESPACE.matcher(text).replaceAll(" ").trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * Cuts {@code text} to at most {@code maxLength} chars, at the last word boundary when there is one.
     */
    public static String truncateAtWordBoundary(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        int cut = text.lastIndexOf(' ', maxLength);
        String result = cut > 0 ? text.substring(0, cut) : text.substring(0, maxLength);
        return result.trim();
    }

    /**
     * First candidate that is non-blank after HTML stripping, cut to {@link #MAX_DESCRIPTION_LENGTH}.
     */
    public static String cleanDescription(String... candidates) {
        for (String candidate : candidates) {
            String text = stripHtml(candidate);
            if (text != null) {
                return truncateAtWordBoundary(text, MAX_DESCRIPTION_LENGTH);
            }
        }
        return null;
    }

    /**
     * Widest {@code 16_9} image, else the widest image, else null. Images without a url are ignored.
     */
    public static String chooseImageUrl(List<TmImage> images) {
        if (images == null) {
            return null;
        }
        List<TmImage> usable = images.stream()
                .filter(Objects::nonNull)
                .filter(image -> StringUtils.hasText(image.getUrl()))
                .toList();
        Comparator<TmImage> byWidth = Comparator.comparingInt(image -> image.getWidth() != null ? image.getWidth() : 0);
        return usable.stream()
                .filter(image -> PREFERRED_IMAGE_RATIO.equals(image.getRatio()))
                .max(byWidth)
                .or(() -> usable.stream().max(byWidth))
                .map(TmImage::getUrl)
                .orElse(null);
    }

    /**
     * Merges price ranges that have a min or a max: smallest min and largest max, each bound
     * falling back to the other. Empty when no range has a price (price unknown, never 0).
     */
    public static Optional<MergedPrice> mergePrices(List<TmPriceRange> ranges) {
        if (ranges == null) {
            return Optional.empty();
        }
        BigDecimal min = null;
        BigDecimal max = null;
        String currency = null;
        for (TmPriceRange range : ranges) {
            if (range == null) {
                continue;
            }
            BigDecimal low = nonNegative(range.getMin() != null ? range.getMin() : range.getMax());
            BigDecimal high = nonNegative(range.getMax() != null ? range.getMax() : range.getMin());
            if (low == null && high == null) {
                continue;
            }
            low = low != null ? low : high;
            high = high != null ? high : low;
            min = min == null || low.compareTo(min) < 0 ? low : min;
            max = max == null || high.compareTo(max) > 0 ? high : max;
            if (currency == null && StringUtils.hasText(range.getCurrency())) {
                currency = range.getCurrency().trim().toUpperCase(Locale.ROOT);
            }
        }
        return min == null ? Optional.empty() : Optional.of(new MergedPrice(min, max, currency));
    }

    /**
     * True for an absolute {@code https} URL with a host.
     */
    public static boolean isHttpsUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        try {
            URI uri = new URI(url.trim());
            return "https".equalsIgnoreCase(uri.getScheme()) && StringUtils.hasText(uri.getHost());
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * First of the given IANA zone ids that is valid, else {@code fallback}.
     */
    public static ZoneId firstValidZone(ZoneId fallback, String... zoneIds) {
        for (String zoneId : zoneIds) {
            if (StringUtils.hasText(zoneId)) {
                try {
                    return ZoneId.of(zoneId.trim());
                } catch (DateTimeException ignored) {
                    // invalid id: try the next one
                }
            }
        }
        return fallback;
    }

    /**
     * Joins the non-blank parts with {@code ", "}; null if all are blank.
     */
    public static String joinNonBlank(String... parts) {
        StringBuilder joined = new StringBuilder();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                if (!joined.isEmpty()) {
                    joined.append(", ");
                }
                joined.append(part.trim());
            }
        }
        return joined.isEmpty() ? null : joined.toString();
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        return value != null && value.signum() >= 0 ? value : null;
    }

    private static String decodeEntities(String text) {
        return text.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }

    /**
     * Result of {@link #mergePrices}; {@code currency} may be null.
     */
    @Value
    public static class MergedPrice {
        BigDecimal min;
        BigDecimal max;
        String currency;
    }
}
