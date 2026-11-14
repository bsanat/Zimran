package kz.astyq.zimran.event.service;

import kz.astyq.zimran.event.model.domain.DemoEventDraft;
import kz.astyq.zimran.event.model.domain.Event;

import java.util.List;

/**
 * The fictional sample events shown while {@code app.demo.enabled=true} (provider {@code demo}, {@code isDemo}).
 */
public interface DemoEventService {

    /**
     * The sample events, dated relative to today in the pilot city.
     */
    List<Event> sampleEvents();

    /**
     * Stores {@link #sampleEvents()} in the catalog (re-dating existing samples).
     *
     * @return how many samples were stored
     */
    int seed();

    /**
     * Adds one new sample event (provider {@code demo}, {@code isDemo}) through ingestion, so it is new to the
     * catalog for real (its {@code firstSeenAt} is now). It does not trigger alert matching: demo events are matched
     * only by the demo alert check.
     *
     * @return the stored sample
     */
    Event addSample(DemoEventDraft draft);
}
