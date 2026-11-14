package kz.astyq.zimran.event.model.domain;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

/**
 * Spring application event published after an ingestion run inserted events the catalog did not have before
 * (consumed by alerts / notifications). Published after the rows are committed; never empty.
 */
@Value
public class NewEventsIngestedEvent {

    @NonNull
    SupportedCity city;
    /** The newly inserted events, as the source returned them. */
    @NonNull
    List<Event> events;
}
