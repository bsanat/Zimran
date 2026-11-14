package kz.astyq.zimran.buddy.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * A new post as the guest wrote it (stripped; the services validate the content).
 */
@Value
@Builder
public class BuddyPostDraft {

    @NonNull
    String eventId;
    @NonNull
    String displayName;
    @NonNull
    String shortBio;
    String language;
    String preferences;
    Integer ageMin;
    Integer ageMax;
}
