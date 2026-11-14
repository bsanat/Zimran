package kz.astyq.zimran.moderation.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Moderation message keys in {@code messages*.properties}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageCode {

    public static final String TARGET_NOT_FOUND = "moderation_target_not_found";
    public static final String CANNOT_BLOCK_SELF = "moderation_cannot_block_self";
    public static final String CANNOT_REPORT_SELF = "moderation_cannot_report_self";
    public static final String BLOCK_NOT_FOUND = "moderation_block_not_found";
}
