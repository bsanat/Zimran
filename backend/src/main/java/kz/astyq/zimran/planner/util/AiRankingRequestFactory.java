package kz.astyq.zimran.planner.util;

import kz.astyq.zimran.ai.model.domain.AiCandidate;
import kz.astyq.zimran.ai.model.domain.AiRankingRequest;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.planner.model.domain.Candidate;
import kz.astyq.zimran.planner.model.domain.ValidatedPlanRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Turns a validated plan request and its candidates into the AI ranking input. Only facts the planner has are
 * passed on; unknown values stay null.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AiRankingRequestFactory {

    private static final DateTimeFormatter LOCAL_START = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm (EEE)", Locale.ENGLISH);

    /**
     * @param withSuggestions whether the model also writes tips for the guest
     */
    public static AiRankingRequest create(ValidatedPlanRequest request, List<Candidate> candidates, String language,
                                          int maxPicks, boolean withSuggestions) {
        return AiRankingRequest.builder()
                .language(language)
                .city(request.getCity().getName())
                .interests(request.getInterests().stream().sorted().map(Interest::getId).toList())
                .startDate(request.getStartDate().toString())
                .endDate(request.getEndDate().toString())
                .timePreference(request.getTimePreference().getId())
                .budget(budget(request.getBudgetMax()))
                .notes(request.getNotes())
                .message(request.getMessage())
                .candidates(candidates.stream().map(candidate -> candidate(candidate, request)).toList())
                .maxPicks(maxPicks)
                .withSuggestions(withSuggestions)
                .build();
    }

    /**
     * One candidate as the model sees it; a multi-day event shows its first day within the request dates.
     */
    public static AiCandidate candidate(Candidate candidate, ValidatedPlanRequest request) {
        Event event = candidate.getEvent();
        return AiCandidate.builder()
                .id(event.getId().asString())
                .title(event.getTitle())
                .category(event.getCategorySlug().getId())
                .interests(event.getInterests().stream().sorted().map(Interest::getId).toList())
                .start(start(event, request))
                .venue(event.getVenue())
                .price(price(event))
                .description(event.getDescription())
                .build();
    }

    private static String start(Event event, ValidatedPlanRequest request) {
        if (event.getSchedule() != null) {
            // A multi-day event: its first day within the plan, plus the source's own wording of the schedule.
            LocalDate day = event.firstDayBetween(request.getStartDate(), request.getEndDate(),
                    request.getCity().getZone());
            String when = day != null ? day + " (" + day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + ")" : null;
            String text = event.getDateText();
            if (when == null) {
                return text;
            }
            return text != null ? when + "; " + text : when + " (time unknown)";
        }
        if (event.getStartAt() != null) {
            return LOCAL_START.format(event.getStartAt().atZoneSameInstant(event.getTimezone()));
        }
        return event.getLocalDate() != null ? event.getLocalDate() + " (time unknown)" : null;
    }

    private static String price(Event event) {
        if (!event.isPriceKnown() || event.getPriceMin() == null) {
            return null;
        }
        String currency = event.getCurrency() != null ? " " + event.getCurrency() : "";
        if (event.getPriceMin().signum() == 0 && (event.getPriceMax() == null || event.getPriceMax().signum() == 0)) {
            return "free";
        }
        if (event.getPriceMax() != null && event.getPriceMax().compareTo(event.getPriceMin()) > 0) {
            return event.getPriceMin().toPlainString() + "-" + event.getPriceMax().toPlainString() + currency;
        }
        return event.getPriceMin().toPlainString() + currency;
    }

    /**
     * The budget in words for the model; null = no limit.
     */
    public static String budget(BigDecimal budgetMax) {
        if (budgetMax == null) {
            return null;
        }
        return budgetMax.signum() == 0 ? "free events only" : "up to " + budgetMax.toPlainString() + " USD per ticket";
    }
}
