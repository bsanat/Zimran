package kz.astyq.zimran.pair.model.dto;

import kz.astyq.zimran.event.model.dto.EventViewResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The event both sides liked ({@code MATCHED}).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PairSharedPlanViewResponse {

    private EventViewResponse event;
    private String reasonA;
    private String reasonB;
}
