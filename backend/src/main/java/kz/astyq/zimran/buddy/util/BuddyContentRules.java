package kz.astyq.zimran.buddy.util;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

/**
 * No contact data is exchanged in the MVP: free text that carries an e-mail address, a phone number or a messenger
 * link is rejected (people meet through the app and in public places).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BuddyContentRules {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    /** Nine or more digits, optionally with spaces, dots, dashes or brackets between them (dates have eight). */
    private static final Pattern PHONE = Pattern.compile("(?:\\+?\\d[\\s().-]{0,2}){9,}");
    private static final Pattern MESSENGER = Pattern.compile(
            "(?i)\\b(?:t\\.me|wa\\.me|telegram\\.me|whatsapp\\.com|instagram\\.com|facebook\\.com|vk\\.com)\\b");

    public static boolean containsContactDetails(String text) {
        return text != null && (EMAIL.matcher(text).find() || PHONE.matcher(text).find()
                || MESSENGER.matcher(text).find());
    }

    /**
     * {@code 400 VALIDATION_ERROR} with {@code details.field = field} when {@code text} carries contact data.
     */
    public static void requireNoContactDetails(String text, String field) {
        if (containsContactDetails(text)) {
            throw new ServiceValidationException(ErrorCode.VALIDATION_ERROR, MessageCode.CONTACT_DETAILS)
                    .withDetail("field", field);
        }
    }
}
