package kz.astyq.zimran.assistant.model.domain;

import kz.astyq.zimran.assistant.model.enums.RelaxationSuggestion;
import kz.astyq.zimran.planner.model.domain.Recommendation;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

/**
 * The assistant's answer: at most 5 candidate events, best first, and ways to loosen the filters when there are
 * none. Only {@code summary} and each recommendation's reason may be model text.
 */
@Value
@Builder
public class AssistantRecommendation {

    /** The ranked plan's id; null when there were no candidates (no AI call). */
    String planId;

    @NonNull
    String summary;
    @NonNull
    List<Recommendation> recommendations;
    /** Empty unless {@code recommendations} is empty. */
    @NonNull
    List<RelaxationSuggestion> relaxations;
}
