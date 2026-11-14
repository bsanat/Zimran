package kz.astyq.zimran.buddy.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * One of the caller's posts with the requests it received, oldest first.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuddyMyPostViewResponse {

    private BuddyPostViewResponse post;
    private List<BuddyIncomingRequestViewResponse> requests;
}
