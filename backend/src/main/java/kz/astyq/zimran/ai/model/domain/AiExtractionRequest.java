package kz.astyq.zimran.ai.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

/**
 * What the model extracts search filters from. {@code message} is untrusted guest text.
 */
@Value
@Builder
public class AiExtractionRequest {

    @NonNull
    String message;
    /** Today in the timezone of the city the guest is looking at; relative dates are resolved against it. */
    @NonNull
    LocalDate today;
    /** The only category ids the model may return as interests. */
    @NonNull
    List<String> categories;
}
