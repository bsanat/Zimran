package kz.astyq.zimran.buddy.service.impl;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.buddy.model.domain.BuddyEventSlot;
import kz.astyq.zimran.buddy.service.BuddyEventService;
import kz.astyq.zimran.buddy.util.MessageCode;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.service.EventQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class BuddyEventServiceImpl implements BuddyEventService {

    private final EventQueryService eventQueryService;
    private final Clock clock;

    @Override
    public BuddyEventSlot requireUpcoming(String eventId) {
        EventId id = EventId.parse(eventId);
        Event event = eventQueryService.findById(id).orElseThrow(() -> new ServiceValidationException(
                ErrorCode.NOT_FOUND, kz.astyq.zimran.event.util.MessageCode.EVENT_NOT_FOUND));
        if (event.getStatus() != EventStatus.SCHEDULED) {
            throw notUpcoming();
        }
        Instant now = clock.instant();
        ZoneId zone = event.getTimezone();
        LocalTime time = event.getStartAt() != null ? event.getStartAt().atZoneSameInstant(zone).toLocalTime() : null;
        LocalDate day = event.firstDayBetween(LocalDate.now(clock.withZone(zone)), null, zone);
        // Today's occurrence may already have started; then the next one counts (later days start after now).
        while (day != null) {
            Instant start = time != null ? day.atTime(time).atZone(zone).toInstant()
                    : day.plusDays(1).atStartOfDay(zone).toInstant();
            if (start.isAfter(now)) {
                return BuddyEventSlot.builder().eventId(event.getId()).expiresAt(start).build();
            }
            day = event.firstDayBetween(day.plusDays(1), null, zone);
        }
        throw notUpcoming();
    }

    private static ServiceValidationException notUpcoming() {
        return new ServiceValidationException(ErrorCode.VALIDATION_ERROR, MessageCode.EVENT_NOT_UPCOMING)
                .withDetail("field", "eventId");
    }
}
