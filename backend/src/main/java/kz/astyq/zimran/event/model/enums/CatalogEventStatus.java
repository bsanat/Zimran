package kz.astyq.zimran.event.model.enums;

/**
 * Status of a row in the {@code events} table. {@link #ACTIVE} is the stored form of {@link EventStatus#SCHEDULED}.
 */
public enum CatalogEventStatus {
    ACTIVE,
    CANCELLED,
    POSTPONED,
    EXPIRED;

    public static CatalogEventStatus of(EventStatus status) {
        return switch (status) {
            case SCHEDULED -> ACTIVE;
            case CANCELLED -> CANCELLED;
            case POSTPONED -> POSTPONED;
            case EXPIRED -> EXPIRED;
        };
    }

    public EventStatus toEventStatus() {
        return switch (this) {
            case ACTIVE -> EventStatus.SCHEDULED;
            case CANCELLED -> EventStatus.CANCELLED;
            case POSTPONED -> EventStatus.POSTPONED;
            case EXPIRED -> EventStatus.EXPIRED;
        };
    }
}
