package kz.astyq.zimran.ai.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

/**
 * Cleans text going to and coming from the model.
 * <ul>
 *   <li>{@link #forPrompt}: untrusted input (guest notes, event text). Control characters and markup that could
 *   imitate prompt structure are removed and the length is capped. The text is then sent as a JSON string value,
 *   never spliced into the instructions.</li>
 *   <li>{@link #fromModel}: model output shown to the guest. Plain single-line text: no HTML, no markdown
 *   emphasis or links, no URLs (the model must not add links), capped length.</li>
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AiTextSanitizer {

    private static final Pattern CONTROL = Pattern.compile("[\\p{Cc}\\p{Cf}]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern FENCES = Pattern.compile("`{3,}|#{3,}|={3,}|-{3,}");
    private static final Pattern URLS = Pattern.compile("(?i)\\b(?:https?://|www\\.)\\S+");
    private static final Pattern MARKDOWN = Pattern.compile("[*_`#>\\[\\]]");

    public static String forPrompt(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        String cleaned = FENCES.matcher(TAGS.matcher(CONTROL.matcher(text).replaceAll(" ")).replaceAll(" "))
                .replaceAll(" ");
        return finish(cleaned, maxLength);
    }

    public static String fromModel(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String cleaned = CONTROL.matcher(text).replaceAll(" ");
        cleaned = TAGS.matcher(cleaned).replaceAll(" ");
        cleaned = URLS.matcher(cleaned).replaceAll(" ");
        cleaned = MARKDOWN.matcher(cleaned).replaceAll("");
        String result = finish(cleaned, maxLength);
        return result != null ? result : "";
    }

    private static String finish(String text, int maxLength) {
        String collapsed = WHITESPACE.matcher(text).replaceAll(" ").strip();
        if (collapsed.isEmpty()) {
            return null;
        }
        if (collapsed.codePointCount(0, collapsed.length()) <= maxLength) {
            return collapsed;
        }
        return collapsed.substring(0, collapsed.offsetByCodePoints(0, maxLength)).strip();
    }
}
