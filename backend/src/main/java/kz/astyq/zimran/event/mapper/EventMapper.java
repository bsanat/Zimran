package kz.astyq.zimran.event.mapper;

import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.dto.EventDetailsViewResponse;
import kz.astyq.zimran.event.model.dto.EventViewResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.time.ZoneId;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EventMapper {

    EventViewResponse toViewResponse(Event event);

    List<EventViewResponse> toViewResponses(List<Event> events);

    EventDetailsViewResponse toDetailsViewResponse(Event event);

    default String map(EventId id) {
        return id != null ? id.asString() : null;
    }

    default String map(ZoneId zone) {
        return zone != null ? zone.getId() : null;
    }
}
