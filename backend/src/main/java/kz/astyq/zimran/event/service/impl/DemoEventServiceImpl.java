package kz.astyq.zimran.event.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.event.model.domain.DemoEventSeed;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.event.service.DemoEventService;
import kz.astyq.zimran.event.service.EventIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

/**
 * Sample events from {@value #RESOURCE}: fictional titles and venues, no source URL or image, in the pilot city.
 */
@Service
@RequiredArgsConstructor
public class DemoEventServiceImpl implements DemoEventService {

    static final String RESOURCE = "demo/demo-events.json";

    private final JsonMapper jsonMapper;
    private final SupportedCityService supportedCityService;
    private final EventIngestionService eventIngestionService;
    private final Clock clock;

    @Override
    public List<Event> sampleEvents() {
        SupportedCity city = supportedCityService.getDefault();
        LocalDate today = LocalDate.now(clock.withZone(city.getZone()));
        return readSeeds().stream().map(seed -> toEvent(seed, city, today)).toList();
    }

    @Override
    public int seed() {
        List<Event> events = sampleEvents();
        eventIngestionService.upsert(events);
        return events.size();
    }

    private List<DemoEventSeed> readSeeds() {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            return jsonMapper.readValue(in, new TypeReference<List<DemoEventSeed>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + RESOURCE, e);
        }
    }

    private static Event toEvent(DemoEventSeed seed, SupportedCity city, LocalDate today) {
        LocalDate day = today.plusDays(seed.getDayOffset());
        Interest primary = seed.getInterests().getFirst();
        boolean priceKnown = seed.getPriceMin() != null;
        return Event.builder()
                .id(new EventId(EventSource.DEMO, seed.getId()))
                .title(seed.getTitle())
                .description(seed.getDescription())
                .category(primary)
                .interests(EnumSet.copyOf(seed.getInterests()))
                .city(city.getName())
                .state(city.getStateCode())
                .timezone(city.getZone())
                .status(EventStatus.SCHEDULED)
                .startAt(day.atTime(seed.getTime()).atZone(city.getZone()).toOffsetDateTime())
                .localDate(day)
                .venue(seed.getVenue())
                .address(seed.getAddress())
                .priceMin(seed.getPriceMin())
                .priceMax(priceKnown ? seed.getPriceMax() : null)
                .currency(priceKnown ? seed.getCurrency() : null)
                .priceKnown(priceKnown)
                .demo(true)
                .build();
    }
}
