package kz.astyq.zimran.assistant.model.domain;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * What an assistant chat knows so far, merged over its turns. Any field may still be unknown (null / empty).
 * Interests use the planner's vocabulary, so the chat can tell "running" from "fitness".
 */
@Value
@Builder(toBuilder = true)
public class AssistantChatPreferences {

    public static final AssistantChatPreferences EMPTY = AssistantChatPreferences.builder().interests(Set.of()).build();

    SupportedCity city;
    @NonNull
    Set<Interest> interests;
    LocalDate dateFrom;
    LocalDate dateTo;
    /** Null = any time of day. */
    TimePreference timePreference;
    /** Null = no budget limit. */
    BigDecimal budgetMax;
    String notes;
}
