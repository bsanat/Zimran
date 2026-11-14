package kz.astyq.zimran.event.mapper;

import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.entity.CatalogEvent;
import kz.astyq.zimran.event.model.enums.CatalogEventStatus;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.event.util.EventDays;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;

/**
 * {@link Event} ⇄ {@link CatalogEvent}. Ingestion bookkeeping ({@code firstSeenAt}, {@code fetchedAt}) and the
 * audit columns are set by the ingestion service, never here.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, imports = EventDays.class)
public interface CatalogEventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "provider", source = "id.source.code")
    @Mapping(target = "providerEventId", source = "id.sourceId")
    @Mapping(target = "interest", source = "category")
    @Mapping(target = "interests", expression = "java(copyInterests(event.getInterests()))")
    @Mapping(target = "categorySlug", source = "categorySlug")
    @Mapping(target = "venueName", source = "venue")
    @Mapping(target = "venueAddress", source = "address")
    @Mapping(target = "startsOn", expression = "java(EventDays.firstDay(event))")
    @Mapping(target = "endsOn", expression = "java(EventDays.lastDay(event))")
    @Mapping(target = "firstSeenAt", ignore = true)
    @Mapping(target = "fetchedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    CatalogEvent toEntity(Event event);

    @InheritConfiguration(name = "toEntity")
    @Mapping(target = "provider", ignore = true)
    @Mapping(target = "providerEventId", ignore = true)
    void updateEntity(Event event, @MappingTarget CatalogEvent entity);

    @Mapping(target = "id", expression = "java(toEventId(entity))")
    @Mapping(target = "category", source = "interest")
    @Mapping(target = "interests", expression = "java(copyInterests(entity.getInterests()))")
    @Mapping(target = "venue", source = "venueName")
    @Mapping(target = "address", source = "venueAddress")
    @Mapping(target = "startAt", expression = "java(toStartAt(entity))")
    Event toDomain(CatalogEvent entity);

    default EventId toEventId(CatalogEvent entity) {
        EventSource source = EventSource.fromCode(entity.getProvider())
                .orElseThrow(() -> new IllegalStateException("Unknown provider in catalog row " + entity.getId()));
        return new EventId(source, entity.getProviderEventId());
    }

    /**
     * The stored instant in the venue's timezone, as the provider sent it.
     */
    default OffsetDateTime toStartAt(CatalogEvent entity) {
        return entity.getStartAt() != null
                ? entity.getStartAt().atZone(ZoneId.of(entity.getTimezone())).toOffsetDateTime() : null;
    }

    /**
     * A new set, so an update replaces the stored set instead of mutating it.
     */
    default Set<Interest> copyInterests(Set<Interest> interests) {
        Set<Interest> copy = EnumSet.noneOf(Interest.class);
        if (interests != null) {
            copy.addAll(interests);
        }
        return copy;
    }

    default Instant map(OffsetDateTime dateTime) {
        return dateTime != null ? dateTime.toInstant() : null;
    }

    default String map(ZoneId zone) {
        return zone != null ? zone.getId() : null;
    }

    default ZoneId map(String zone) {
        return zone != null ? ZoneId.of(zone) : null;
    }

    default CatalogEventStatus map(EventStatus status) {
        return status != null ? CatalogEventStatus.of(status) : null;
    }

    default EventStatus map(CatalogEventStatus status) {
        return status != null ? status.toEventStatus() : null;
    }
}
