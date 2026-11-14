package kz.astyq.zimran.ticketmaster.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Price range; absent when Ticketmaster does not know the price.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmPriceRange {

    private String type;
    private String currency;
    private BigDecimal min;
    private BigDecimal max;
}
