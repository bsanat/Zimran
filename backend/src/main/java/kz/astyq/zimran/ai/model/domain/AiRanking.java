package kz.astyq.zimran.ai.model.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

/**
 * A schema-valid model answer, sanitized. Picks are in the model's order and still unverified against the
 * candidates.
 */
@Value
@Builder
public class AiRanking {

    /** May be empty. */
    @NonNull
    String summary;
    @NonNull
    List<AiPick> picks;
    @NonNull
    List<String> suggestions;
}
