package kz.astyq.zimran.ai.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

/**
 * What the model ranks: the guest's preferences and the candidate events. {@code notes} is untrusted guest text.
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
    @NonNull
    List<AiCandidate> candidates;
    int maxPicks;
}
