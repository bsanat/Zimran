package kz.astyq.zimran.ticketmaster.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Raw response of {@code GET /discovery/v2/events.json}. {@code _embedded} is absent when nothing matched.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmSearchResponse {

    @JsonProperty("_embedded")
    private TmEmbeddedEvents embedded;

    private TmPage page;

    /**
     * Events of this page, never null.
     */
    public List<TmEvent> events() {
        return embedded != null && embedded.getEvents() != null ? embedded.getEvents() : List.of();
    }
}
