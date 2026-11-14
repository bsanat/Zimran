package kz.astyq.zimran.event.model.enums;

/**
 * Interests a guest can pick. Which Ticketmaster classifications cover each one is configured in
 * {@code app.ticketmaster.interest-mapping}; an interest without coverage yields no events.
 */
public enum Interest {
    MUSIC,
    SPORTS,
    THEATER,
    ART,
    COMEDY,
    FOOD,
    NIGHTLIFE,
    OUTDOORS,
    TECHNOLOGY,
    GAMING
}
