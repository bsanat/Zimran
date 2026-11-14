package kz.astyq.zimran.event.mapper;

import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.dto.EventViewResponse;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import kz.astyq.zimran.event.model.enums.Interest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EventMapper {

    /** ISO-8601 with offset and whole seconds, e.g. {@code 2026-11-20T20:00:00-05:00}. */
    DateTimeFormatter START_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    @Mapping(target = "source", source = "id.source.code")
    @Mapping(target = "sourceEventId", source = "id.sourceId")
    EventViewResponse toViewResponse(Event event);

    List<EventViewResponse> toViewResponses(List<Event> events);

    default String map(EventId id) {
        return id != null ? id.asString() : null;
    }

    default String map(Interest interest) {
        return interest != null ? interest.getId() : null;
    }

    default String map(CategorySlug slug) {
        return slug != null ? slug.getId() : null;
    }

    default String map(ZoneId zone) {
        return zone != null ? zone.getId() : null;
    }

    default String map(OffsetDateTime dateTime) {
        return dateTime != null ? START_AT_FORMAT.format(dateTime) : null;
    }
}
