package kz.astyq.zimran.event.util;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventCriteria;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.model.enums.Interest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/**
 * Whether one event matches {@link EventCriteria}. Pure functions shared by the catalog query and ingestion (which
 * uses {@link #inCity}).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventFilter {

    public static boolean matches(Event event, EventCriteria criteria) {
        String keyword = StringUtils.hasText(criteria.getKeyword())
                ? criteria.getKeyword().strip().toLowerCase(Locale.ROOT) : null;
        Set<Interest> interests = criteria.getInterests() != null ? criteria.getInterests() : Set.of();
        return inCity(event, criteria.getCity())
                && (criteria.isIncludeInactive() || isActive(event))
                && inRange(event, criteria)
                && (interests.isEmpty() || !Collections.disjoint(event.getInterests(), interests))
                && (criteria.getCategorySlug() == null || event.getCategorySlug() == criteria.getCategorySlug())
                && (keyword == null || containsKeyword(event, keyword))
                && (!criteria.isMajorOnly() || event.isMajor())
                && fitsPrice(event, criteria);
    }

    /**
     * Same city name; when both the event and the city have a state, the same state too (two cities may share a
     * name, e.g. Portland OR and Portland ME).
     */
    public static boolean inCity(Event event, SupportedCity city) {
        if (!event.getCity().strip().equalsIgnoreCase(city.getName())) {
            return false;
        }
        return !StringUtils.hasText(event.getState()) || !StringUtils.hasText(city.getStateCode())
                || event.getState().strip().equalsIgnoreCase(city.getStateCode().strip());
    }

    /**
     * Not cancelled and not expired.
     */
    public static boolean isActive(Event event) {
        return event.getStatus() != EventStatus.CANCELLED && event.getStatus() != EventStatus.EXPIRED;
    }

    /**
     * Whether the event happens on at least one day of the range (a multi-day event matches when any of its
     * days does). Undated events cannot be checked and are kept only when the criteria ask for them.
     */
    static boolean inRange(Event event, EventCriteria criteria) {
        if (!event.isDated()) {
            return criteria.isIncludeUndated();
        }
        if (criteria.getFromDate() == null && criteria.getToDate() == null) {
            return true;
        }
        return event.firstDayBetween(criteria.getFromDate(), criteria.getToDate(), criteria.getCity().getZone()) != null;
    }

    /**
     * Unknown price never passes a budget or "free only"; free means the source says the lowest and highest
     * price are 0.
     */
    static boolean fitsPrice(Event event, EventCriteria criteria) {
        if (criteria.getBudgetMax() == null && !criteria.isFreeOnly()) {
            return true;
        }
        BigDecimal min = event.getPriceMin();
        if (!event.isPriceKnown() || min == null) {
            return false;
        }
        if (criteria.isFreeOnly()
                && (min.signum() != 0 || (event.getPriceMax() != null && event.getPriceMax().signum() != 0))) {
            return false;
        }
        return criteria.getBudgetMax() == null || min.compareTo(criteria.getBudgetMax()) <= 0;
    }

    private static boolean containsKeyword(Event event, String keyword) {
        return contains(event.getTitle(), keyword)
                || contains(event.getDescription(), keyword)
                || contains(event.getVenue(), keyword);
    }

    private static boolean contains(String text, String keyword) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
