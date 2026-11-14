package kz.astyq.zimran.ticketmaster.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Event start. {@code localDate}/{@code localTime} are venue-local; {@code dateTime} is a UTC instant.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmStart {

    private String localDate;
    private String localTime;
    private String dateTime;
    private Boolean dateTBD;
    private Boolean timeTBA;
    private Boolean noSpecificTime;
}
