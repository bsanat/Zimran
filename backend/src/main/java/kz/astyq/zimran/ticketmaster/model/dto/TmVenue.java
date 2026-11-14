package kz.astyq.zimran.ticketmaster.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Event venue.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmVenue {

    private String name;
    private TmAddress address;
    private TmNamed city;
    private TmState state;
    private String postalCode;
    private String timezone;
}
