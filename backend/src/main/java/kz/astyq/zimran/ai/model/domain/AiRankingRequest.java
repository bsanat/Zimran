package kz.astyq.zimran.ai.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

/**
 * What the model ranks: the guest's preferences and the candidate events. {@code notes} and {@code message} are
 * untrusted guest text.
 */
@Value
@Builder
public class AiRankingRequest {

    /** ISO 639-1 code of the language to write in ({@code en}, {@code ru}, {@code kk}). */
    @NonNull
    String language;
    @NonNull
    String city;
    @NonNull
    List<String> interests;
    @NonNull
    String startDate;
    @NonNull
    String endDate;
    @NonNull
    String timePreference;
    /** e.g. {@code up to 50 USD}, {@code free only}; null = no limit. */
    String budget;
    String notes;
    /** The guest's original free-text request, when there is one (AI assistant); null otherwise. */
    String message;
    @NonNull
    List<AiCandidate> candidates;
    int maxPicks;
    /** Whether the model also writes tips for the guest; false = the answer schema has no suggestions. */
    boolean withSuggestions;
}
