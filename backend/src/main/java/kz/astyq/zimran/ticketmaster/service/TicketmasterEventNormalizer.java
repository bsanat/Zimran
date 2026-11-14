package kz.astyq.zimran.ticketmaster.service;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.ticketmaster.model.dto.TmEvent;

import java.util.Optional;

public interface TicketmasterEventNormalizer {

    /**
     * Converts a raw Ticketmaster event. Empty when a required field (id, name, https url, a definite
     * start) is missing; nothing is ever made up. No I/O.
     */
    Optional<Event> normalize(TmEvent tm, SupportedCity city);
}
