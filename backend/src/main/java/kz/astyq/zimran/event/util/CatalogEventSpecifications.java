package kz.astyq.zimran.event.util;

import jakarta.persistence.criteria.Predicate;
import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.model.domain.EventCriteria;
import kz.astyq.zimran.event.model.entity.CatalogEvent;
import kz.astyq.zimran.event.model.enums.CatalogEventStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SQL pre-filters of the catalog. They never drop a row {@link EventFilter} would keep: the date bounds are widened
 * by a day because rows store days in the venue's timezone while criteria use the city's.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CatalogEventSpecifications {

    private static final Set<CatalogEventStatus> INACTIVE =
            EnumSet.of(CatalogEventStatus.CANCELLED, CatalogEventStatus.EXPIRED);

    /**
     * Rows of {@code city} (state checked when the row has one); demo rows only when {@code includeDemo}.
     */
    public static Specification<CatalogEvent> inCity(SupportedCity city, boolean includeDemo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(cb.lower(cb.trim(root.get("city"))), city.getName().strip().toLowerCase(Locale.ROOT)));
            if (StringUtils.hasText(city.getStateCode())) {
                predicates.add(cb.or(cb.isNull(root.get("state")), cb.equal(cb.lower(cb.trim(root.get("state"))),
                        city.getStateCode().strip().toLowerCase(Locale.ROOT))));
            }
            if (!includeDemo) {
                predicates.add(cb.isFalse(root.get("demo")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Rows of {@code city} from a real provider (never demo rows).
     */
    public static Specification<CatalogEvent> providerRowsInCity(SupportedCity city) {
        return inCity(city, false);
    }

    /**
     * Rows of {@code city} that are neither cancelled nor expired.
     */
    public static Specification<CatalogEvent> activeInCity(SupportedCity city, boolean includeDemo) {
        Specification<CatalogEvent> inCity = inCity(city, includeDemo);
        return (root, query, cb) -> cb.and(inCity.toPredicate(root, query, cb), cb.not(root.get("status").in(INACTIVE)));
    }

    /**
     * Rows that may match {@code criteria}; {@link EventFilter#matches} decides on the exact days, interests,
     * keyword and major flag.
     */
    public static Specification<CatalogEvent> matching(EventCriteria criteria, boolean includeDemo) {
        Specification<CatalogEvent> city = criteria.isIncludeInactive()
                ? inCity(criteria.getCity(), includeDemo) : activeInCity(criteria.getCity(), includeDemo);
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(city.toPredicate(root, query, cb));
            if (criteria.getCategorySlug() != null) {
                predicates.add(cb.equal(root.get("categorySlug"), criteria.getCategorySlug()));
            }
            if (criteria.getFromDate() != null) {
                predicates.add(cb.or(cb.isNull(root.get("endsOn")),
                        cb.greaterThanOrEqualTo(root.get("endsOn"), criteria.getFromDate().minusDays(1))));
            }
            if (criteria.getToDate() != null) {
                predicates.add(cb.or(cb.isNull(root.get("startsOn")),
                        cb.lessThanOrEqualTo(root.get("startsOn"), criteria.getToDate().plusDays(1))));
            }
            if (criteria.getBudgetMax() != null || criteria.isFreeOnly()) {
                // Unknown price never passes a budget or "free only".
                predicates.add(cb.isTrue(root.get("priceKnown")));
                predicates.add(cb.isNotNull(root.get("priceMin")));
            }
            if (criteria.getBudgetMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("priceMin"), criteria.getBudgetMax()));
            }
            if (criteria.isFreeOnly()) {
                predicates.add(cb.equal(root.get("priceMin"), BigDecimal.ZERO));
                predicates.add(cb.or(cb.isNull(root.get("priceMax")), cb.equal(root.get("priceMax"), BigDecimal.ZERO)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
