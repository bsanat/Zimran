package kz.astyq.zimran.planner.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.configuration.PlannerProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import kz.astyq.zimran.planner.model.dto.PlanCreateRequest;
import kz.astyq.zimran.planner.model.enums.TimePreference;
import kz.astyq.zimran.planner.service.PlanRequestValidationService;
import kz.astyq.zimran.planner.util.MessageCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PlanRequestValidationServiceImpl implements PlanRequestValidationService {

    static final int MAX_NOTES_LENGTH = 500;

    /** Control characters other than the newline. */
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cc}&&[^\\n]]");
    /** Runs of horizontal whitespace. */
    private static final Pattern SPACES = Pattern.compile("[\\p{Zs}]+");

    private final SupportedCityService supportedCityService;
    private final PlannerProperties plannerProperties;
    private final Clock clock;

    @Override
    public ValidatedPlanRequest validate(PlanCreateRequest request) {
        Objects.requireNonNull(request, "request");
        SupportedCity city = supportedCityService.getByName(request.getCity(), request.getState());
        Set<Interest> interests = interests(request);

        LocalDate today = LocalDate.now(clock.withZone(city.getZone()));
        LocalDate startDate = startDate(request.getStartDate(), today);
        LocalDate endDate = endDate(request.getEndDate(), startDate);

        return ValidatedPlanRequest.builder()
                .city(city)
                .interests(interests)
                .startDate(startDate)
                .endDate(endDate)
                .budgetMax(budget(request.getBudgetMax()))
                .timePreference(request.getTimePreference() != null ? request.getTimePreference() : TimePreference.ANY)
                .notes(notes(request.getNotes()))
                .build();
    }

    private static Set<Interest> interests(PlanCreateRequest request) {
        Set<Interest> interests = EnumSet.noneOf(Interest.class);
        if (request.getInterests() != null) {
            request.getInterests().stream().filter(Objects::nonNull).forEach(interests::add);
        }
        if (interests.isEmpty()) {
            throw invalid(MessageCode.INTERESTS_REQUIRED, "interests");
        }
        return Collections.unmodifiableSet(interests);
    }

    private static LocalDate startDate(LocalDate startDate, LocalDate today) {
        if (startDate == null) {
            throw invalid(MessageCode.START_DATE_REQUIRED, "startDate");
        }
        if (startDate.isBefore(today)) {
            throw invalid(MessageCode.START_DATE_IN_PAST, "startDate");
        }
        return startDate;
    }

    private LocalDate endDate(LocalDate endDate, LocalDate startDate) {
        if (endDate == null) {
            return startDate;
        }
        if (endDate.isBefore(startDate)) {
            throw invalid(MessageCode.END_DATE_BEFORE_START, "endDate");
        }
        int maxRangeDays = plannerProperties.getMaxRangeDays();
        if (ChronoUnit.DAYS.between(startDate, endDate) + 1 > maxRangeDays) {
            throw invalid(MessageCode.DATE_RANGE_TOO_LONG, "endDate", maxRangeDays);
        }
        return endDate;
    }

    private static BigDecimal budget(BigDecimal budgetMax) {
        if (budgetMax == null) {
            return null;
        }
        if (budgetMax.signum() < 0) {
            throw invalid(MessageCode.BUDGET_INVALID, "budgetMax");
        }
        return budgetMax;
    }

    /**
     * Removes control characters except newlines, collapses runs of spaces and trims; blank becomes null.
     * The length limit applies to the cleaned text.
     */
    private static String notes(String notes) {
        if (notes == null) {
            return null;
        }
        String cleaned = CONTROL_CHARS.matcher(notes).replaceAll("");
        cleaned = SPACES.matcher(cleaned).replaceAll(" ").strip();
        if (cleaned.isEmpty()) {
            return null;
        }
        if (cleaned.codePointCount(0, cleaned.length()) > MAX_NOTES_LENGTH) {
            throw invalid(MessageCode.NOTES_TOO_LONG, "notes", MAX_NOTES_LENGTH);
        }
        return cleaned;
    }

    private static ServiceValidationException invalid(String messageCode, String field, Object... args) {
        return new ServiceValidationException(ErrorCode.VALIDATION_ERROR, messageCode, args)
                .withDetail("field", field);
    }
}
