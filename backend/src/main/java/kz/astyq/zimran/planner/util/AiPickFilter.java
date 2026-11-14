package kz.astyq.zimran.planner.util;

import kz.astyq.zimran.ai.model.domain.AiPick;
import kz.astyq.zimran.planner.model.domain.Candidate;
import kz.astyq.zimran.planner.model.domain.Recommendation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Keeps only AI picks that name one of the candidates sent to the model: unknown ids and repeated ids are
 * dropped, the model's order is kept, at most {@code max} remain. The event itself always comes from the
 * candidate (source data), never from the model's text.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AiPickFilter {

    /**
     * @param fallbackReason reason used when the model gave an empty one
     */
    public static List<Recommendation> filter(List<AiPick> picks, Map<String, Candidate> candidatesById, int max,
                                              Function<Candidate, String> fallbackReason) {
        List<Recommendation> recommendations = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (AiPick pick : picks) {
            if (recommendations.size() >= max) {
                break;
            }
            Candidate candidate = candidatesById.get(pick.getEventId());
            if (candidate == null || !seen.add(pick.getEventId())) {
                continue;
            }
            String reason = pick.getReason().isBlank() ? fallbackReason.apply(candidate) : pick.getReason();
            recommendations.add(Recommendation.builder()
                    .event(candidate.getEvent())
                    .matchScore(Math.max(0, Math.min(100, pick.getMatchScore())))
                    .reason(reason)
                    .build());
        }
        return recommendations;
    }
}
