package kz.astyq.zimran.event.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import kz.astyq.zimran.core.model.AuditableEntity;
import kz.astyq.zimran.event.model.domain.EventSchedule;
import kz.astyq.zimran.event.model.entity.converter.EventScheduleConverter;
import kz.astyq.zimran.event.model.entity.converter.InterestSetConverter;
import kz.astyq.zimran.event.model.enums.CatalogEventStatus;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import kz.astyq.zimran.event.model.enums.Interest;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/**
 * One event of the persistent catalog, identified by {@code provider} + {@code providerEventId} (the public id is
 * {@code <provider>:<providerEventId>}; the numeric id stays internal). Every value comes from the provider.
 */
@Entity
@Table(name = "events")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SequenceGenerator(name = "seq", sequenceName = "s_events", allocationSize = 1)
@SQLDelete(sql = "update events set is_deleted = true, deleted_at = now() where id = ?")
@SQLRestriction("is_deleted = false")
public class CatalogEvent extends AuditableEntity {

    /** {@code EventSource} code, e.g. {@code json} or {@code demo}. */
    @Column(name = "provider", nullable = false, updatable = false, length = 32)
    private String provider;

    @Column(name = "provider_event_id", nullable = false, updatable = false)
    private String providerEventId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_slug", nullable = false, length = 32)
    private CategorySlug categorySlug;

    /** Primary interest; {@link #categorySlug} is its slug. */
    @Enumerated(EnumType.STRING)
    @Column(name = "interest", nullable = false, length = 32)
    private Interest interest;

    @Convert(converter = InterestSetConverter.class)
    @Column(name = "interests", nullable = false, length = 512)
    private Set<Interest> interests;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", length = 8)
    private String state;

    @Column(name = "venue_name")
    private String venueName;

    @Column(name = "venue_address")
    private String venueAddress;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "local_date")
    private LocalDate localDate;

    @Column(name = "date_text")
    private String dateText;

    @Convert(converter = EventScheduleConverter.class)
    @Column(name = "schedule")
    private EventSchedule schedule;

    /** First day the event can happen on, in {@link #timezone}; null = open. */
    @Column(name = "starts_on")
    private LocalDate startsOn;

    /** Last day the event can happen on, in {@link #timezone}; null = open. */
    @Column(name = "ends_on")
    private LocalDate endsOn;

    /** IANA zone of the venue. */
    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    @Column(name = "price_min")
    private BigDecimal priceMin;

    @Column(name = "price_max")
    private BigDecimal priceMax;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "price_known", nullable = false)
    private boolean priceKnown;

    @Column(name = "price_text")
    private String priceText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CatalogEventStatus status;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_major", nullable = false)
    private boolean major;

    @Column(name = "is_demo", nullable = false)
    private boolean demo;

    /** When ingestion first inserted the row; never changed afterwards. */
    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt;

    /** When the provider last returned the event. */
    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;
}
