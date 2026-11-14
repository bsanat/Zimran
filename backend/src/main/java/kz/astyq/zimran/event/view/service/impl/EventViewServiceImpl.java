package kz.astyq.zimran.event.view.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.mapper.EventMapper;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventCriteria;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.dto.EventDetailsViewResponse;
import kz.astyq.zimran.event.model.dto.EventPageViewResponse;
import kz.astyq.zimran.event.model.dto.EventSearchRequest;
import kz.astyq.zimran.event.model.dto.PaginationViewResponse;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.event.service.EventQueryService;
import kz.astyq.zimran.event.util.MessageCode;
import kz.astyq.zimran.event.view.service.EventViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventViewServiceImpl implements EventViewService {

    private final EventQueryService eventQueryService;
    private final SupportedCityService supportedCityService;
    private final EventMapper eventMapper;
    private final Clock clock;

    /**
     * Without {@code date}: every event from today on, plus events whose date the source does not know.
     * With {@code date}: only events on that day.
     */
    @Override
    public EventPageViewResponse getPage(EventSearchRequest request) {
        SupportedCity city = supportedCityService.getByName(request.getCity(), request.getState());
        LocalDate today = LocalDate.now(clock.withZone(city.getZone()));
        LocalDate date = request.getDate();
        if (date != null && date.isBefore(today)) {
            throw invalid(MessageCode.DATE_IN_PAST, "date");
        }

        Set<Interest> interests = request.getCategory() != null
                ? EnumSet.of(request.getCategory()) : EnumSet.noneOf(Interest.class);
        List<Event> events = eventQueryService.search(EventCriteria.builder()
                .city(city)
                .fromDate(date != null ? date : today)
                .toDate(date)
                .includeUndated(date == null)
                .interests(interests)
                .keyword(request.getKeyword())
                .majorOnly("true".equals(request.getMajor()))
                .build());

        int page = request.getPage() != null ? request.getPage() : EventSearchRequest.DEFAULT_PAGE;
        int pageSize = EventSearchRequest.PAGE_SIZE;
        long from = (long) (page - 1) * pageSize;
        int fromIndex = (int) Math.min(from, events.size());
        int toIndex = (int) Math.min(from + pageSize, events.size());

        return EventPageViewResponse.builder()
                .events(eventMapper.toViewResponses(events.subList(fromIndex, toIndex)))
                .pagination(PaginationViewResponse.builder()
                        .page(page)
                        .pageSize(pageSize)
                        .hasMore(toIndex < events.size())
                        .build())
                .build();
    }

    @Override
    public EventDetailsViewResponse getById(String id) {
        EventId eventId = EventId.parse(id);
        return eventQueryService.findById(eventId)
                .map(event -> EventDetailsViewResponse.builder().event(eventMapper.toViewResponse(event)).build())
                .orElseThrow(() -> new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.EVENT_NOT_FOUND));
    }

    private static ServiceValidationException invalid(String messageCode, String field) {
        return new ServiceValidationException(ErrorCode.VALIDATION_ERROR, messageCode).withDetail("field", field);
    }
}
