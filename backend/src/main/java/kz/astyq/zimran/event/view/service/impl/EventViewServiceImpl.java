package kz.astyq.zimran.event.view.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.mapper.EventMapper;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.dto.EventDetailsViewResponse;
import kz.astyq.zimran.event.model.dto.EventPageViewResponse;
import kz.astyq.zimran.event.model.dto.EventSearchRequest;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.event.service.EventQueryService;
import kz.astyq.zimran.event.util.MessageCode;
import kz.astyq.zimran.event.view.service.EventViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventViewServiceImpl implements EventViewService {

    /** Longest allowed range, counting both the first and the last day. */
    static final int MAX_RANGE_DAYS = 7;

    private final EventQueryService eventQueryService;
    private final SupportedCityService supportedCityService;
    private final EventMapper eventMapper;
    private final Clock clock;

    @Override
    public EventPageViewResponse getPage(EventSearchRequest request) {
        SupportedCity city = supportedCityService.getByName(request.getCity());
        LocalDate today = LocalDate.ofInstant(clock.instant(), city.getZone());

        boolean single = request.getDate() != null;
        LocalDate startDate = single ? request.getDate()
                : request.getStartDate() != null ? request.getStartDate() : today;
        LocalDate endDate = single ? request.getDate()
                : request.getEndDate() != null ? request.getEndDate() : startDate;

        if (endDate.isBefore(startDate)) {
            throw invalid(MessageCode.DATE_RANGE_INVALID, "endDate");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_RANGE_DAYS) {
            throw invalid(MessageCode.DATE_RANGE_TOO_LONG, "endDate", MAX_RANGE_DAYS);
        }
        if (startDate.isBefore(today)) {
            throw invalid(MessageCode.DATE_IN_PAST, single ? "date" : "startDate");
        }

        List<Event> events = eventQueryService.findEvents(city, startDate, endDate, interests(request.getCategory()));

        int page = request.getPage() != null ? request.getPage() : EventSearchRequest.DEFAULT_PAGE;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : EventSearchRequest.DEFAULT_PAGE_SIZE;
        long from = (long) (page - 1) * pageSize;
        int fromIndex = (int) Math.min(from, events.size());
        int toIndex = (int) Math.min(from + pageSize, events.size());

        return EventPageViewResponse.builder()
                .items(eventMapper.toViewResponses(events.subList(fromIndex, toIndex)))
                .page(page)
                .pageSize(pageSize)
                .total(events.size())
                .hasMore(toIndex < events.size())
                .build();
    }

    @Override
    public EventDetailsViewResponse getById(String id) {
        EventId eventId = EventId.parse(id);
        return eventQueryService.findById(eventId)
                .map(eventMapper::toDetailsViewResponse)
                .orElseThrow(() -> new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.EVENT_NOT_FOUND, id));
    }

    private static Set<Interest> interests(List<Interest> category) {
        if (category == null) {
            return EnumSet.noneOf(Interest.class);
        }
        List<Interest> present = category.stream().filter(Objects::nonNull).toList();
        return present.isEmpty() ? EnumSet.noneOf(Interest.class) : EnumSet.copyOf(present);
    }

    private static ServiceValidationException invalid(String messageCode, String field, Object... arguments) {
        return new ServiceValidationException(ErrorCode.VALIDATION_ERROR, messageCode, arguments)
                .withDetail("field", field);
    }
}
