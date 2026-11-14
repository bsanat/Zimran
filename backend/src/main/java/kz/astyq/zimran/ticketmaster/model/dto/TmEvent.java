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
 * Raw Ticketmaster Discovery v2 event; every field may be null. Date/time values stay as the strings
 * Ticketmaster sends. {@code info}, {@code pleaseNote} and {@code description} are untrusted text.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmEvent {

    private String id;
    private String name;
    private String url;
    private String info;
    private String pleaseNote;
    private String description;
    private List<TmImage> images;
    private TmDates dates;
    private List<TmClassification> classifications;
    private List<TmPriceRange> priceRanges;

    @JsonProperty("_embedded")
    private TmEventEmbedded embedded;
}
