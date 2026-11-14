package kz.astyq.zimran.assistant.service.impl;

import kz.astyq.zimran.ai.model.domain.AiExtraction;
import kz.astyq.zimran.ai.model.domain.AiExtractionRequest;
import kz.astyq.zimran.ai.service.AiFilterExtractionService;
import kz.astyq.zimran.assistant.model.domain.AssistantChatPreferences;
import kz.astyq.zimran.assistant.model.domain.AssistantFilters;
import kz.astyq.zimran.assistant.model.enums.AssistantChatField;
import kz.astyq.zimran.assistant.service.AssistantChatPreferenceService;
import kz.astyq.zimran.assistant.service.AssistantFilterResolutionService;
import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AssistantChatPreferenceServiceImpl implements AssistantChatPreferenceService {

    /** The chat asks the model for planner interests (finer than the category slugs of POST /api/ai/parse). */
    static final List<String> INTEREST_IDS = Arrays.stream(Interest.values()).map(Interest::getId).toList();

    private final AiFilterExtractionService aiFilterExtractionService;
    private final AssistantFilterResolutionService assistantFilterResolutionService;
    private final Clock clock;

    @Override
    public AssistantChatPreferences merge(String message, AssistantChatPreferences previous) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(previous, "previous");
        String previousCity = previous.getCity() != null ? previous.getCity().getId() : null;
        SupportedCity referenceCity = assistantFilterResolutionService.referenceCity(previousCity);
        AiExtraction extraction = aiFilterExtractionService.extract(AiExtractionRequest.builder()
                .message(message)
                .today(today(referenceCity))
                .categories(INTEREST_IDS)
                .build());
        // City, dates, time and budget are validated like POST /api/ai/parse; interests are read separately below.
        AssistantFilters filters = assistantFilterResolutionService.resolve(extraction, previousCity);

        AssistantChatPreferences.AssistantChatPreferencesBuilder merged = previous.toBuilder().city(filters.getCity());
        Set<Interest> interests = interests(extraction.getInterests());
        if (!interests.isEmpty()) {
            merged.interests(interests);
        }
        // A date the message states replaces the previous range, even when it is invalid (then it is asked again).
        if (extraction.getDateFrom() != null || extraction.getDateTo() != null) {
            merged.dateFrom(filters.getDateFrom()).dateTo(filters.getDateTo());
        }
        TimePreference timePreference = filters.getTimeOfDay();
        if (timePreference != null) {
            merged.timePreference(timePreference);
        }
        BigDecimal budgetMax = filters.getBudgetMax();
        if (budgetMax != null) {
            merged.budgetMax(budgetMax);
        }
        if (filters.getNotes() != null) {
            merged.notes(filters.getNotes());
        }
        return merged.build();
    }

    @Override
    public List<AssistantChatField> missingFields(AssistantChatPreferences preferences) {
        List<AssistantChatField> missing = new ArrayList<>();
        if (preferences.getCity() == null) {
            missing.add(AssistantChatField.CITY);
        }
        if (preferences.getInterests().isEmpty()) {
            missing.add(AssistantChatField.INTERESTS);
        }
        // A chat can outlive its dates: a range that has started in the past is asked again.
        LocalDate today = today(preferences.getCity() != null ? preferences.getCity()
                : assistantFilterResolutionService.referenceCity(null));
        if (preferences.getDateFrom() == null || preferences.getDateTo() == null
                || preferences.getDateFrom().isBefore(today)) {
            missing.add(AssistantChatField.DATES);
        }
        return List.copyOf(missing);
    }

    private LocalDate today(SupportedCity city) {
        return LocalDate.now(clock.withZone(city.getZone()));
    }

    private static Set<Interest> interests(List<String> ids) {
        Set<Interest> interests = EnumSet.noneOf(Interest.class);
        ids.stream().map(Interest::fromId).flatMap(Optional::stream).forEach(interests::add);
        return Collections.unmodifiableSet(interests);
    }
}
