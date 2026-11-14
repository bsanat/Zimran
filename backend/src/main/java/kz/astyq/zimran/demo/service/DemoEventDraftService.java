package kz.astyq.zimran.demo.service;

import kz.astyq.zimran.demo.model.dto.DemoEventCreateRequest;
import kz.astyq.zimran.event.model.domain.DemoEventDraft;

/**
 * Resolves a demo event request into a draft: city, start time and title defaults.
 */
public interface DemoEventDraftService {

    /**
     * Unsupported city: {@code 400 VALIDATION_ERROR} ({@code details.field=city}). A start that is not in the
     * future: {@code 400 VALIDATION_ERROR} ({@code details.field=startAt}).
     */
    DemoEventDraft resolve(DemoEventCreateRequest request);
}
