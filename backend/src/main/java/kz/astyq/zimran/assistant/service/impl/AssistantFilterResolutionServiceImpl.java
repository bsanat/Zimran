package kz.astyq.zimran.assistant.service.impl;

import kz.astyq.zimran.ai.model.domain.AiExtraction;
import kz.astyq.zimran.assistant.model.domain.AssistantFilters;
import kz.astyq.zimran.assistant.model.enums.AssistantField;
import kz.astyq.zimran.assistant.service.AssistantFilterResolutionService;
import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.configuration.PlannerProperties;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AssistantFilterResolutionServiceImpl implements AssistantFilterResolutionService {

    private final SupportedCityService supportedCityService;
    private final PlannerProperties plannerProperties;
    private final Clock clock;

    @Override
    public SupportedCity referenceCity(String requestedCity) {
        return findCity(requestedCity).orElseGet(supportedCityService::getDefault);
    }

    @Override
    public AssistantFilters resolve(AiExtraction extraction, String requestedCity) {
        Objects.requireNonNull(extraction, "extraction");
        Set<AssistantField> missing = EnumSet.noneOf(AssistantField.class);

        SupportedCity city = city(extraction, requestedCity);
        if (city == null) {
            missing.add(AssistantField.CITY);
        }
        Set<CategorySlug> interests = interests(extraction.getInterests());
        if (interests.isEmpty()) {
            missing.add(AssistantField.INTERESTS);
        }

        LocalDate today = LocalDate.now(clock.withZone((city != null ? city : referenceCity(requestedCity)).getZone()));
        LocalDate dateFrom = date(extraction.getDateFrom());
        if (dateFrom != null && dateFrom.isBefore(today)) {
            dateFrom = null;
        }
        LocalDate dateTo = extraction.getDateTo() == null ? dateFrom : date(extraction.getDateTo());
        if (dateTo != null && !isValidEnd(dateTo, dateFrom, today)) {
            dateTo = null;
        }
        if (dateFrom == null) {
            missing.add(AssistantField.DATE_FROM);
        }
        if (dateTo == null) {
            missing.add(AssistantField.DATE_TO);
        }

        TimePreference timeOfDay = null;
        if (extraction.getTimeOfDay() != null) {
            timeOfDay = timePreference(extraction.getTimeOfDay()).orElse(null);
            if (timeOfDay == null) {
                missing.add(AssistantField.TIME_OF_DAY);
            }
        }
        BigDecimal budgetMax = extraction.getBudgetMax();
        if (budgetMax != null && budgetMax.signum() < 0) {
            budgetMax = null;
            missing.add(AssistantField.BUDGET_MAX);
        }

        return AssistantFilters.builder()
                .city(city)
                .interests(interests)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .timeOfDay(timeOfDay)
                .budgetMax(budgetMax)
                .notes(StringUtils.hasText(extraction.getNotes()) ? extraction.getNotes().strip() : null)
                .missingFields(List.copyOf(missing))
                .build();
    }

    /**
     * A city named in the message wins; an unsupported one stays missing instead of silently becoming another.
     */
    private SupportedCity city(AiExtraction extraction, String requestedCity) {
        String named = extraction.getCity();
        if (StringUtils.hasText(named)) {
            return supportedCityService.findByName(named, extraction.getState())
                    .or(() -> supportedCityService.findById(named))
                    .orElse(null);
        }
        return findCity(requestedCity).orElse(null);
    }

    private Optional<SupportedCity> findCity(String idOrName) {
        if (!StringUtils.hasText(idOrName)) {
            return Optional.empty();
        }
        return supportedCityService.findById(idOrName.strip()).or(() -> supportedCityService.findByName(idOrName));
    }

    private static Set<CategorySlug> interests(List<String> ids) {
        Set<CategorySlug> slugs = EnumSet.noneOf(CategorySlug.class);
        ids.stream().map(CategorySlug::fromId).flatMap(Optional::stream).forEach(slugs::add);
        return Collections.unmodifiableSet(slugs);
    }

    private static LocalDate date(String text) {
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text.strip());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private boolean isValidEnd(LocalDate dateTo, LocalDate dateFrom, LocalDate today) {
        if (dateTo.isBefore(today)) {
            return false;
        }
        return dateFrom == null || (!dateTo.isBefore(dateFrom)
                && ChronoUnit.DAYS.between(dateFrom, dateTo) + 1 <= plannerProperties.getMaxRangeDays());
    }

    private static Optional<TimePreference> timePreference(String id) {
        String wanted = id.strip();
        return Arrays.stream(TimePreference.values()).filter(value -> value.name().equalsIgnoreCase(wanted)).findFirst();
    }
}
