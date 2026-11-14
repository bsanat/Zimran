package kz.astyq.zimran.ai.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

/**
 * One event as the model sees it. Text fields come from the event source and are untrusted.
 */
@Value
@Builder
public class AiCandidate {

    @NonNull
    String id;
    @NonNull
    String title;
    @NonNull
    String category;
    @NonNull
    List<String> interests;
    /** Local start, e.g. {@code 2026-11-20 20:00}, or only the day; null when unknown. */
    String start;
    String venue;
    /** e.g. {@code 25 USD}, {@code free}, or null when unknown. */
    String price;
    String description;
}
