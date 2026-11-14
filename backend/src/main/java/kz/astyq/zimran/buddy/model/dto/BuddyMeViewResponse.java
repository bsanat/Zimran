package kz.astyq.zimran.buddy.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuddyMeViewResponse {

    /** The caller's latest posts, newest first. */
    private List<BuddyMyPostViewResponse> posts;
    /** The caller's latest requests, newest first. */
    private List<BuddyOutgoingRequestViewResponse> requests;
}
