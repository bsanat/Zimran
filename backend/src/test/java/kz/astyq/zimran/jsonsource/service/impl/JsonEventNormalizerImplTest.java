package kz.astyq.zimran.jsonsource.service.impl;

import kz.astyq.zimran.city.service.impl.SupportedCityServiceImpl;
import kz.astyq.zimran.core.configuration.CityProperties;
import kz.astyq.zimran.core.configuration.EventSourceProperties;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.model.enums.Interest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonEventNormalizerImplTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    private JsonEventNormalizerImpl normalizer;

    @BeforeEach
    void setUp() {
        EventSourceProperties sourceProperties = new EventSourceProperties();
        sourceProperties.setCategoryMapping(Map.of(Interest.MUSIC, List.of("Concerts"), Interest.THEATER, List.of("Broadway")));
        CityProperties cityProperties = new CityProperties();
        cityProperties.setDefaultId("nyc");
        cityProperties.setSupported(List.of(CityProperties.CityDefinition.builder()
                .id("nyc").name("New York").stateCode("NY").countryCode("US").zone(NEW_YORK).build()));
        normalizer = new JsonEventNormalizerImpl(new CategoryMappingServiceImpl(sourceProperties),
                new SupportedCityServiceImpl(cityProperties));
    }

    @Nested
    @DisplayName("normalize")
    class Normalize {

        @Test
        @DisplayName("a full record keeps every value from the source")
        void full() {
            Event event = one("""
                    {"id": "evt-1", "title": " Jazz  Night ", "categories": ["Concerts", "nightlife"],
                     "city": "New York", "startAt": "2026-11-20T20:00:00-05:00", "timezone": "America/New_York",
                     "venue": {"name": "Blue Note", "address": "131 W 3rd St"},
                     "description": "<p>Two <b>sets</b> &amp; more</p>", "imageUrl": "https://img.example.com/a.jpg",
                     "url": "https://tickets.example.com/e/1", "priceMin": 25, "priceMax": "60.00",
                     "currency": "usd", "priceText": "$25-$60", "status": "onsale", "isMajor": true}
                    """);

            assertThat(event.getId().asString()).isEqualTo("json:evt-1");
            assertThat(event.getTitle()).isEqualTo("Jazz Night");
            assertThat(event.getCategory()).isEqualTo(Interest.MUSIC);
            assertThat(event.getInterests()).containsExactlyInAnyOrder(Interest.MUSIC, Interest.NIGHTLIFE);
            assertThat(event.getCity()).isEqualTo("New York");
            assertThat(event.getStartAt()).isEqualTo(OffsetDateTime.parse("2026-11-20T20:00:00-05:00"));
            assertThat(event.getLocalDate()).isEqualTo(LocalDate.of(2026, 11, 20));
            assertThat(event.getVenue()).isEqualTo("Blue Note");
            assertThat(event.getAddress()).isEqualTo("131 W 3rd St");
            assertThat(event.getDescription()).isEqualTo("Two sets & more");
            assertThat(event.getImageUrl()).isEqualTo("https://img.example.com/a.jpg");
            assertThat(event.getSourceUrl()).isEqualTo("https://tickets.example.com/e/1");
            assertThat(event.getPriceMin()).isEqualByComparingTo("25");
            assertThat(event.getPriceMax()).isEqualByComparingTo("60");
            assertThat(event.isPriceKnown()).isTrue();
            assertThat(event.getCurrency()).isEqualTo("USD");
            assertThat(event.getPriceText()).isEqualTo("$25-$60");
            assertThat(event.getStatus()).isEqualTo(EventStatus.SCHEDULED);
            assertThat(event.isMajor()).isTrue();
        }

        @Test
        @DisplayName("missing optional fields stay null: no price, URL, image, time or venue is made up")
        void minimal() {
            Event event = one("{\"id\": \"m1\", \"title\": \"Show\", \"category\": \"music\"}");

            assertThat(event.getStartAt()).isNull();
            assertThat(event.getLocalDate()).isNull();
            assertThat(event.getDateText()).isNull();
            assertThat(event.getVenue()).isNull();
            assertThat(event.getAddress()).isNull();
            assertThat(event.getDescription()).isNull();
            assertThat(event.getImageUrl()).isNull();
            assertThat(event.getSourceUrl()).isNull();
            assertThat(event.getPriceMin()).isNull();
            assertThat(event.getPriceMax()).isNull();
            assertThat(event.getCurrency()).isNull();
            assertThat(event.isPriceKnown()).isFalse();
            assertThat(event.isMajor()).isFalse();
            assertThat(event.getStatus()).isEqualTo(EventStatus.SCHEDULED);
            assertThat(event.getCity()).isEqualTo("New York");
            assertThat(event.getTimezone()).isEqualTo(NEW_YORK);
        }

        @ParameterizedTest
        @ValueSource(strings = {"http://img.example.com/a.jpg", "https://user:pw@img.example.com/a.jpg",
                "https://img.example.com:8443/a.jpg", "javascript:alert(1)", "not a url", "/relative.jpg"})
        @DisplayName("unsafe image and source URLs become null")
        void unsafeUrls(String url) {
            Event event = one("{\"id\":\"u\",\"title\":\"T\",\"category\":\"music\",\"imageUrl\":\"" + url
                    + "\",\"sourceUrl\":\"" + url + "\"}");

            assertThat(event.getImageUrl()).isNull();
            assertThat(event.getSourceUrl()).isNull();
        }

        @Test
        @DisplayName("https on port 443 is kept")
        void port443() {
            Event event = one("{\"id\":\"u\",\"title\":\"T\",\"category\":\"music\",\"url\":\"https://x.example.com:443/e\"}");

            assertThat(event.getSourceUrl()).isEqualTo("https://x.example.com:443/e");
        }

        @Test
        @DisplayName("UTC start is converted to the event zone; a local date-time is read in it")
        void timezones() {
            Event utc = one("{\"id\":\"a\",\"title\":\"T\",\"category\":\"music\",\"startAt\":\"2026-11-21T04:30:00Z\"}");
            Event local = one("{\"id\":\"b\",\"title\":\"T\",\"category\":\"music\",\"start\":\"2026-07-01T19:00\"}");

            assertThat(utc.getStartAt()).isEqualTo(OffsetDateTime.parse("2026-11-20T23:30:00-05:00"));
            assertThat(utc.getLocalDate()).isEqualTo(LocalDate.of(2026, 11, 20));
            assertThat(local.getStartAt()).isEqualTo(OffsetDateTime.parse("2026-07-01T19:00:00-04:00"));
        }

        @Test
        @DisplayName("date + time fields, date only, and unparsable text kept as dateText")
        void dateForms() {
            Event dateTime = one("{\"id\":\"a\",\"title\":\"T\",\"category\":\"music\",\"date\":\"2026-11-20\",\"time\":\"09:15\"}");
            Event dateOnly = one("{\"id\":\"b\",\"title\":\"T\",\"category\":\"music\",\"date\":\"2026-11-20\"}");
            Event text = one("{\"id\":\"c\",\"title\":\"T\",\"category\":\"music\",\"startAt\":\"Fri, Nov 20\"}");

            assertThat(dateTime.getStartAt()).isEqualTo(OffsetDateTime.parse("2026-11-20T09:15:00-05:00"));
            assertThat(dateOnly.getStartAt()).isNull();
            assertThat(dateOnly.getLocalDate()).isEqualTo(LocalDate.of(2026, 11, 20));
            assertThat(dateOnly.getDateText()).isEqualTo("2026-11-20");
            assertThat(dateTime.getDateText()).isNull();
            assertThat(text.getStartAt()).isNull();
            assertThat(text.getLocalDate()).isNull();
            assertThat(text.getDateText()).isEqualTo("Fri, Nov 20");
        }

        @Test
        @DisplayName("price rules: free, single price, min only, inconsistent range, negative, text only")
        void prices() {
            Event free = one("{\"id\":\"a\",\"title\":\"T\",\"category\":\"music\",\"isFree\":true}");
            Event freeText = one("{\"id\":\"b\",\"title\":\"T\",\"category\":\"music\",\"price\":\"Free\"}");
            Event single = one("{\"id\":\"c\",\"title\":\"T\",\"category\":\"music\",\"price\":30,\"currency\":\"USD\"}");
            Event minOnly = one("{\"id\":\"d\",\"title\":\"T\",\"category\":\"music\",\"priceMin\":10}");
            Event inverted = one("{\"id\":\"e\",\"title\":\"T\",\"category\":\"music\",\"priceMin\":50,\"priceMax\":10}");
            Event negative = one("{\"id\":\"f\",\"title\":\"T\",\"category\":\"music\",\"priceMin\":-5}");
            Event textOnly = one("{\"id\":\"g\",\"title\":\"T\",\"category\":\"music\",\"price\":\"From $20\"}");

            assertThat(free.getPriceMin()).isZero();
            assertThat(free.getPriceMax()).isZero();
            assertThat(free.getCurrency()).isEqualTo("USD");
            assertThat(freeText.isPriceKnown()).isTrue();
            assertThat(single.getPriceMin()).isEqualByComparingTo("30");
            assertThat(single.getPriceMax()).isEqualByComparingTo("30");
            assertThat(minOnly.getPriceMin()).isEqualByComparingTo("10");
            assertThat(minOnly.getPriceMax()).isNull();
            assertThat(minOnly.getCurrency()).isNull();
            for (Event unknown : List.of(inverted, negative, textOnly)) {
                assertThat(unknown.isPriceKnown()).isFalse();
                assertThat(unknown.getPriceMin()).isNull();
                assertThat(unknown.getPriceMax()).isNull();
            }
            assertThat(textOnly.getPriceText()).isEqualTo("From $20");
        }

        @Test
        @DisplayName("status aliases; a missing id gets a stable hash; unsafe ids are hashed")
        void statusAndIds() {
            Event cancelled = one("{\"id\":\"a\",\"title\":\"T\",\"category\":\"music\",\"status\":\"Canceled\"}");
            Event postponed = one("{\"id\":\"b\",\"title\":\"T\",\"category\":\"music\",\"status\":\"rescheduled\"}");
            Event noId1 = one("{\"title\":\"T\",\"category\":\"music\",\"date\":\"2026-11-20\"}");
            Event noId2 = one("{\"title\":\"T\",\"category\":\"music\",\"date\":\"2026-11-20\"}");
            Event slash = one("{\"id\":\"a/b c\",\"title\":\"T\",\"category\":\"music\"}");

            assertThat(cancelled.getStatus()).isEqualTo(EventStatus.CANCELLED);
            assertThat(postponed.getStatus()).isEqualTo(EventStatus.POSTPONED);
            assertThat(noId1.getId()).isEqualTo(noId2.getId());
            assertThat(noId1.getId().getSourceId()).matches("[0-9a-f]{24}");
            assertThat(slash.getId().getSourceId()).matches("[0-9a-f]{24}");
        }

        @Test
        @DisplayName("an unsupported city is kept as given (so it is not found in New York searches)")
        void otherCity() {
            assertThat(one("{\"id\":\"a\",\"title\":\"T\",\"category\":\"music\",\"city\":\"Boston\"}").getCity())
                    .isEqualTo("Boston");
        }

        @Test
        @DisplayName("no title, unmapped category, unknown status or not an object: skipped")
        void rejected() {
            assertThat(normalizer.normalize(json("{\"id\":\"a\",\"category\":\"music\"}"))).isEmpty();
            assertThat(normalizer.normalize(json("{\"id\":\"a\",\"title\":\"T\",\"category\":\"Pottery\"}"))).isEmpty();
            assertThat(normalizer.normalize(json("{\"id\":\"a\",\"title\":\"T\"}"))).isEmpty();
            assertThat(normalizer.normalize(json("{\"id\":\"a\",\"title\":\"T\",\"category\":\"music\",\"status\":\"zzz\"}"))).isEmpty();
            assertThat(normalizer.normalize(json("\"text\""))).isEmpty();
        }
    }

    @Nested
    @DisplayName("normalizeAll")
    class NormalizeAll {

        @Test
        @DisplayName("top-level array or {events: [...]}; bad records are skipped")
        void shapes() {
            String records = "[{\"id\":\"a\",\"title\":\"A\",\"category\":\"Broadway\"},{\"title\":\"\"},42]";

            assertThat(normalizer.normalizeAll(json(records))).extracting(Event::getCategory).containsExactly(Interest.THEATER);
            assertThat(normalizer.normalizeAll(json("{\"events\":" + records + "}"))).hasSize(1);
        }

        @Test
        @DisplayName("any other document: UPSTREAM_UNAVAILABLE")
        void wrongShape() {
            assertThatThrownBy(() -> normalizer.normalizeAll(json("{\"items\":[]}")))
                    .isInstanceOfSatisfying(ServiceValidationException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPSTREAM_UNAVAILABLE));
        }
    }

    @Nested
    @DisplayName("state, multi-day schedule and image fallback")
    class ScheduleStateImage {

        private static final LocalDate DAY = LocalDate.of(2026, 11, 20); // a Friday

        @Test
        @DisplayName("state is kept; the supported city's own state code wins")
        void state() {
            assertThat(one("""
                    {"title": "A", "category": "concerts", "city": "New York", "state": "ny"}
                    """).getState()).isEqualTo("NY");
            assertThat(one("""
                    {"title": "A", "category": "concerts", "city": "Austin", "venue": {"name": "V", "state": "TX"}}
                    """).getState()).isEqualTo("TX");
        }

        @Test
        @DisplayName("startDate..endDate without startAt: a schedule over every day of the range")
        void dateRange() {
            Event event = one("""
                    {"title": "Show", "category": "concerts", "startAt": null,
                     "startDate": "2026-11-15", "endDate": "2027-01-10", "dateText": "Nov 15 - Jan 10"}
                    """);

            assertThat(event.getStartAt()).isNull();
            assertThat(event.getLocalDate()).isNull();
            assertThat(event.getDateText()).isEqualTo("Nov 15 - Jan 10");
            assertThat(event.isDated()).isTrue();
            assertThat(event.firstDayBetween(DAY, DAY, NEW_YORK)).isEqualTo(DAY);
            assertThat(event.firstDayBetween(LocalDate.of(2027, 1, 11), null, NEW_YORK)).isNull();
        }

        @Test
        @DisplayName("startDate equal to endDate (or alone): a single day, no schedule")
        void singleDay() {
            Event event = one("""
                    {"title": "Show", "category": "concerts", "startDate": "2026-11-20", "endDate": "2026-11-20"}
                    """);

            assertThat(event.getSchedule()).isNull();
            assertThat(event.getLocalDate()).isEqualTo(DAY);
        }

        @Test
        @DisplayName("recurrenceDays (0 = Sunday) and recurrenceMonths restrict the days")
        void weekly() {
            Event event = one("""
                    {"title": "Tour", "category": "concerts", "recurrenceDays": [0, 6], "recurrenceMonths": [11, 12]}
                    """);

            assertThat(event.firstDayBetween(DAY, null, NEW_YORK)).isEqualTo(DAY.plusDays(1));
            assertThat(event.getSchedule().occursOn(DAY.plusDays(2))).isTrue();  // Sunday
            assertThat(event.getSchedule().occursOn(LocalDate.of(2027, 1, 2))).isFalse(); // Saturday in January
        }

        @Test
        @DisplayName("occurrenceDates, availableDateRanges and excludedDates")
        void listedDays() {
            Event listed = one("""
                    {"title": "Play", "category": "concerts", "startDate": "2026-11-21", "endDate": "2026-12-27",
                     "occurrenceDates": ["2026-11-21", "2026-11-25", "bad"], "excludedDates": ["2026-11-25"]}
                    """);
            Event ranged = one("""
                    {"title": "Lights", "category": "concerts",
                     "availableDateRanges": [["2026-11-20", "2026-11-22"], ["2026-12-04", "2026-12-06"]]}
                    """);

            assertThat(listed.firstDayBetween(DAY, null, NEW_YORK)).isEqualTo(LocalDate.of(2026, 11, 21));
            assertThat(listed.firstDayBetween(LocalDate.of(2026, 11, 22), null, NEW_YORK)).isNull();
            assertThat(ranged.firstDayBetween(LocalDate.of(2026, 11, 23), null, NEW_YORK))
                    .isEqualTo(LocalDate.of(2026, 12, 4));
            assertThat(ranged.getSchedule().getFirstDate()).isEqualTo(DAY);
        }

        @Test
        @DisplayName("endDate before startDate, or a recurring record without days: undated")
        void undated() {
            assertThat(one("""
                    {"title": "A", "category": "concerts", "startDate": "2026-12-01", "endDate": "2026-11-01"}
                    """).isDated()).isFalse();
            assertThat(one("""
                    {"title": "A", "category": "concerts", "isRecurring": true, "dateText": "Select dates"}
                    """).isDated()).isFalse();
        }

        @Test
        @DisplayName("a site-relative imageUrl falls back to the https imageSourceUrl")
        void imageFallback() {
            assertThat(one("""
                    {"title": "A", "category": "concerts", "imageUrl": "/catalog/a.webp",
                     "imageSourceUrl": "https://img.example.com/a.jpg"}
                    """).getImageUrl()).isEqualTo("https://img.example.com/a.jpg");
            assertThat(one("""
                    {"title": "A", "category": "concerts", "imageUrl": "/catalog/a.webp"}
                    """).getImageUrl()).isNull();
        }
    }

    private Event one(String record) {
        Optional<Event> event = normalizer.normalize(json(record));
        assertThat(event).isPresent();
        return event.get();
    }

    private static JsonNode json(String text) {
        return JSON.readTree(text);
    }
}
