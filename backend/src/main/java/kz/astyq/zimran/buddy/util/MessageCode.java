package kz.astyq.zimran.buddy.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Event Buddy message keys in {@code messages*.properties}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageCode {

    public static final String POST_NOT_FOUND = "buddy_post_not_found";
    public static final String REQUEST_NOT_FOUND = "buddy_request_not_found";
    public static final String EVENT_NOT_UPCOMING = "buddy_event_not_upcoming";
    public static final String POST_EXISTS = "buddy_post_exists";
    public static final String OWN_POST = "buddy_own_post";
    public static final String POST_NOT_OPEN = "buddy_post_not_open";
    public static final String REQUEST_EXISTS = "buddy_request_exists";
    public static final String BLOCKED = "buddy_blocked";
    public static final String NOT_POST_OWNER = "buddy_not_post_owner";
    public static final String NOT_REQUEST_SENDER = "buddy_not_request_sender";
    public static final String REQUEST_NOT_PENDING = "buddy_request_not_pending";
    public static final String REQUEST_NOT_ACTIVE = "buddy_request_not_active";
    public static final String AGE_CONFIRMATION_REQUIRED = "buddy_age_confirmation_required";
    public static final String AGE_RANGE_INVALID = "buddy_age_range_invalid";
    public static final String CONTACT_DETAILS = "buddy_contact_details_not_allowed";
    public static final String SAFETY_NOTICE = "buddy_safety_notice";
}
