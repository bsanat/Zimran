package kz.astyq.zimran.ticketmaster.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Segment / genre / sub-genre classification.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmClassification {

    private Boolean primary;
    private TmNamed segment;
    private TmNamed genre;
    private TmNamed subGenre;
}
