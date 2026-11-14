package kz.astyq.zimran.buddy.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class BuddyRequestDraft {

    @NonNull
    String displayName;
    @NonNull
    String message;
}
