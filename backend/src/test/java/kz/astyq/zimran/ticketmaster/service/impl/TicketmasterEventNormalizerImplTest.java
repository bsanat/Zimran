package kz.astyq.zimran.ticketmaster.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.event.model.domain.EventId;
import kz.astyq.zimran.event.model.enums.EventSource;
import kz.astyq.zimran.event.model.enums.EventStatus;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.ticketmaster.model.dto.TmAddress;
import kz.astyq.zimran.ticketmaster.model.dto.TmClassification;
import kz.astyq.zimran.ticketmaster.model.dto.TmDates;
import kz.astyq.zimran.ticketmaster.model.dto.TmEvent;
import kz.astyq.zimran.ticketmaster.model.dto.TmEventEmbedded;
import kz.astyq.zimran.ticketmaster.model.dto.TmImage;
import kz.astyq.zimran.ticketmaster.model.dto.TmNamed;
import kz.astyq.zimran.ticketmaster.model.dto.TmPriceRange;
import kz.astyq.zimran.ticketmaster.model.dto.TmSearchResponse;
import kz.astyq.zimran.ticketmaster.model.dto.TmStart;
import kz.astyq.zimran.ticketmaster.model.dto.TmState;
import kz.astyq.zimran.ticketmaster.model.dto.TmStatus;
import kz.astyq.zimran.ticketmaster.model.dto.TmVenue;
import kz.astyq.zimran.ticketmaster.service.InterestMappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketmasterEventNormalizerImplTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final SupportedCity NYC = SupportedCity.builder()
            .id("nyc").name("New York").stateCode("NY").countryCode("US").zone(NEW_YORK).build();

    @Mock
    private InterestMappingService interestMappingService;

    @InjectMocks
    private TicketmasterEventNormalizerImpl normalizer;

    @BeforeEach
    void setUp() {
        lenient().when(interestMappingService.resolve(any())).thenReturn(List.of(Interest.MUSIC));
    }

    @Nested
    @DisplayName("normalize: fixtures")
    class Fixtures {

        @Test
        @DisplayName("search-nyc-music: full event mapped field by field")
        void fullEvent() {
            TmEvent jazz = searchFixture().get(0);

            Event event = normalizer.normalize(jazz, NYC).orElseThrow();

            assertThat(event.getId()).isEqualTo(new EventId(EventSource.TICKETMASTER, "vvG1zZ9KbGkM1a"));
            assertThat(event.getTitle()).isEqualTo("Jazz at the Village Vanguard");
            assertThat(event.getSourceUrl()).isEqualTo("https://www.ticketmaster.com/event/vvG1zZ9KbGkM1a");
            assertThat(event.getStartAt()).isEqualTo(OffsetDateTime.parse("2026-11-20T20:00:00-05:00"));
            assertThat(event.getTimezone()).isEqualTo(NEW_YORK);
            assertThat(event.getStatus()).isEqualTo(EventStatus.SCHEDULED);
            assertThat(event.getDescription()).isEqualTo("Two sets nightly.");
            assertThat(event.getCategory()).isEqualTo("Music");
            assertThat(event.getInterests()).containsExactly(Interest.MUSIC);
            assertThat(event.getVenue()).isEqualTo("Village Vanguard");
            assertThat(event.getAddress()).isEqualTo("178 7th Ave S, New York, NY, 10014");
            assertThat(event.getCity()).isEqualTo("New York");
            assertThat(event.getPriceMin()).isEqualByComparingTo("35");
            assertThat(event.getPriceMax()).isEqualByComparingTo("89.5");
            assertThat(event.getCurrency()).isEqualTo("USD");
            assertThat(event.isPriceKnown()).isTrue();
            assertThat(event.getImageUrl()).isEqualTo("https://s1.ticketm.net/dam/a/123/jazz_16_9_1024.jpg");
            verify(interestMappingService).resolve(jazz.getClassifications());
        }

        @Test
        @DisplayName("search-nyc-music: event without priceRanges has unknown price, not $0")
        void noPriceRanges() {
            Event event = normalizer.normalize(searchFixture().get(1), NYC).orElseThrow();

            assertThat(event.getPriceMin()).isNull();
            assertThat(event.getPriceMax()).isNull();
            assertThat(event.getCurrency()).isNull();
            assertThat(event.isPriceKnown()).isFalse();
            assertThat(event.getCity()).isEqualTo("Brooklyn");
        }

        @Test
        @DisplayName("search-nyc-music: cancelled event with TBA time is dropped (missing_start)")
        void cancelledWithTbaTime() {
            assertThat(normalizer.normalize(searchFixture().get(2), NYC)).isEmpty();
            verify(interestMappingService, never()).resolve(any());
        }

        @Test
        @DisplayName("event-html-rescheduled: HTML, several prices, image choice, venue timezone")
        void htmlRescheduled() {
            Event event = normalizer.normalize(fixture("event-html-rescheduled.json", TmEvent.class), NYC)
                    .orElseThrow();

            assertThat(event.getTitle()).isEqualTo("Comedy Cellar Late Show");
            assertThat(event.getDescription()).isEqualTo("Stand-up comedy all night. Tom & Jerry host.");
            assertThat(event.getStatus()).isEqualTo(EventStatus.POSTPONED);
            assertThat(event.getStartAt()).isEqualTo(OffsetDateTime.parse("2026-11-21T22:30:00-05:00"));
            assertThat(event.getTimezone()).isEqualTo(NEW_YORK);
            assertThat(event.getPriceMin()).isEqualByComparingTo("18.5");
            assertThat(event.getPriceMax()).isEqualByComparingTo("120");
            assertThat(event.getCurrency()).isEqualTo("USD");
            assertThat(event.getImageUrl()).isEqualTo("https://s1.ticketm.net/dam/c/16_9_1136.jpg");
            assertThat(event.getCategory()).isEqualTo("Arts & Theatre");
        }
    }

    @Nested
    @DisplayName("normalize: required fields")
    class RequiredFields {

        @ParameterizedTest
        @ValueSource(strings = {"id", "name", "url"})
        @DisplayName("missing id / name / url: dropped")
        void missingRequired(String field) {
            TmEvent tm = base();
            switch (field) {
                case "id" -> tm.setId(" ");
                case "name" -> tm.setName(null);
                default -> tm.setUrl("");
            }

            assertThat(normalizer.normalize(tm, NYC)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"http://www.ticketmaster.com/event/1", "ftp://x/1", "/event/1", "https://", "not a url"})
        @DisplayName("non-https url: dropped")
        void nonHttpsUrl(String url) {
            TmEvent tm = base();
            tm.setUrl(url);

            assertThat(normalizer.normalize(tm, NYC)).isEmpty();
        }

        @Test
        @DisplayName("null event: empty")
        void nullEvent() {
            assertThat(normalizer.normalize(null, NYC)).isEmpty();
        }
    }

    @Nested
    @DisplayName("normalize: start")
    class Start {

        @Test
        @DisplayName("dateTime wins over local date/time and is shown in the venue timezone")
        void dateTimeWins() {
            TmEvent tm = base();
            tm.getDates().setStart(TmStart.builder()
                    .dateTime("2026-07-04T23:00:00Z").localDate("2026-07-04").localTime("12:00:00").build());

            Event event = normalizer.normalize(tm, NYC).orElseThrow();

            assertThat(event.getStartAt()).isEqualTo(OffsetDateTime.parse("2026-07-04T19:00:00-04:00"));
        }

        @Test
        @DisplayName("without dateTime: localDate + localTime in the timezone")
        void localDateAndTime() {
            TmEvent tm = base();
            tm.getDates().setStart(TmStart.builder().localDate("2026-12-31").localTime("23:30:00").build());

            assertThat(normalizer.normalize(tm, NYC).orElseThrow().getStartAt())
                    .isEqualTo(OffsetDateTime.parse("2026-12-31T23:30:00-05:00"));
        }

        @Test
        @DisplayName("unparseable dateTime falls back to local date/time")
        void badDateTime() {
            TmEvent tm = base();
            tm.getDates().setStart(TmStart.builder()
                    .dateTime("soon").localDate("2026-12-31").localTime("20:00:00").build());

            assertThat(normalizer.normalize(tm, NYC).orElseThrow().getStartAt())
                    .isEqualTo(OffsetDateTime.parse("2026-12-31T20:00:00-05:00"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"dateTBD", "timeTBA", "noSpecificTime"})
        @DisplayName("TBD / TBA / no specific time: dropped even with a dateTime")
        void tba(String flag) {
            TmEvent tm = base();
            TmStart start = tm.getDates().getStart();
            switch (flag) {
                case "dateTBD" -> start.setDateTBD(true);
                case "timeTBA" -> start.setTimeTBA(true);
                default -> start.setNoSpecificTime(true);
            }

            assertThat(normalizer.normalize(tm, NYC)).isEmpty();
        }

        @Test
        @DisplayName("only localDate: dropped")
        void onlyLocalDate() {
            TmEvent tm = base();
            tm.getDates().setStart(TmStart.builder().localDate("2026-12-31").build());

            assertThat(normalizer.normalize(tm, NYC)).isEmpty();
        }

        @Test
        @DisplayName("no dates at all: dropped")
        void noDates() {
            TmEvent tm = base();
            tm.setDates(null);

            assertThat(normalizer.normalize(tm, NYC)).isEmpty();
        }
    }

    @Nested
    @DisplayName("normalize: timezone")
    class Timezone {

        @Test
        @DisplayName("dates.timezone first")
        void datesTimezone() {
            TmEvent tm = base();
            tm.getDates().setTimezone("America/Chicago");
            venue(tm).setTimezone("America/Denver");

            Event event = normalizer.normalize(tm, NYC).orElseThrow();

            assertThat(event.getTimezone()).isEqualTo(ZoneId.of("America/Chicago"));
            assertThat(event.getStartAt()).isEqualTo(OffsetDateTime.parse("2026-11-20T19:00:00-06:00"));
        }

        @Test
        @DisplayName("then venue.timezone")
        void venueTimezone() {
            TmEvent tm = base();
            tm.getDates().setTimezone(null);
            venue(tm).setTimezone("America/Denver");

            assertThat(normalizer.normalize(tm, NYC).orElseThrow().getTimezone()).isEqualTo(ZoneId.of("America/Denver"));
        }

        @Test
        @DisplayName("then the city zone; invalid ids are skipped")
        void cityZone() {
            TmEvent tm = base();
            tm.getDates().setTimezone("Mars/Olympus_Mons");
            venue(tm).setTimezone(null);
            SupportedCity la = SupportedCity.builder().id("la").name("Los Angeles")
                    .zone(ZoneId.of("America/Los_Angeles")).build();

            Event event = normalizer.normalize(tm, la).orElseThrow();

            assertThat(event.getTimezone()).isEqualTo(ZoneId.of("America/Los_Angeles"));
            assertThat(event.getStartAt()).isEqualTo(OffsetDateTime.parse("2026-11-20T17:00:00-08:00"));
        }
    }

    @Nested
    @DisplayName("normalize: status")
    class Status {

        @ParameterizedTest
        @CsvSource({"cancelled,CANCELLED", "Canceled,CANCELLED", "postponed,POSTPONED", "RESCHEDULED,POSTPONED",
                "onsale,SCHEDULED", "offsale,SCHEDULED", "whatever,SCHEDULED"})
        @DisplayName("status code mapped case-insensitively")
        void mapsStatus(String code, EventStatus expected) {
            TmEvent tm = base();
            tm.getDates().setStatus(TmStatus.builder().code(code).build());

            assertThat(normalizer.normalize(tm, NYC).orElseThrow().getStatus()).isEqualTo(expected);
        }

        @Test
        @DisplayName("no status: SCHEDULED")
        void noStatus() {
            TmEvent tm = base();
            tm.getDates().setStatus(null);

            assertThat(normalizer.normalize(tm, NYC).orElseThrow().getStatus()).isEqualTo(EventStatus.SCHEDULED);
        }
    }

    @Nested
    @DisplayName("normalize: price")
    class Price {

        @Test
        @DisplayName("only max: both bounds are the max")
        void onlyMax() {
            Event event = withPrices(TmPriceRange.builder().currency("USD").max(new BigDecimal("60")).build());

            assertThat(event.getPriceMin()).isEqualByComparingTo("60");
            assertThat(event.getPriceMax()).isEqualByComparingTo("60");
            assertThat(event.isPriceKnown()).isTrue();
        }

        @Test
        @DisplayName("several ranges: smallest min, largest max, first currency uppercased")
        void severalRanges() {
            Event event = withPrices(
                    TmPriceRange.builder().currency("usd").min(new BigDecimal("30")).max(new BigDecimal("50")).build(),
                    TmPriceRange.builder().currency("EUR").min(new BigDecimal("10")).max(new BigDecimal("200")).build());

            assertThat(event.getPriceMin()).isEqualByComparingTo("10");
            assertThat(event.getPriceMax()).isEqualByComparingTo("200");
            assertThat(event.getCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("ranges without min and max: price unknown")
        void emptyRanges() {
            Event event = withPrices(TmPriceRange.builder().currency("USD").type("standard").build());

            assertThat(event.getPriceMin()).isNull();
            assertThat(event.getPriceMax()).isNull();
            assertThat(event.getCurrency()).isNull();
            assertThat(event.isPriceKnown()).isFalse();
        }

        @Test
        @DisplayName("an explicit 0 from the source is kept")
        void explicitZero() {
            Event event = withPrices(TmPriceRange.builder().currency("USD").min(BigDecimal.ZERO).max(BigDecimal.ZERO).build());

            assertThat(event.getPriceMin()).isEqualByComparingTo("0");
            assertThat(event.isPriceKnown()).isTrue();
        }

        @Test
        @DisplayName("known price without currency: currency null")
        void noCurrency() {
            Event event = withPrices(TmPriceRange.builder().min(new BigDecimal("15")).build());

            assertThat(event.isPriceKnown()).isTrue();
            assertThat(event.getCurrency()).isNull();
        }

        private Event withPrices(TmPriceRange... ranges) {
            TmEvent tm = base();
            tm.setPriceRanges(List.of(ranges));
            return normalizer.normalize(tm, NYC).orElseThrow();
        }
    }

    @Nested
    @DisplayName("normalize: venue and address")
    class VenueAndAddress {

        @Test
        @DisplayName("missing venue: venue and address null, city from SupportedCity")
        void missingVenue() {
            TmEvent tm = base();
            tm.setEmbedded(null);

            Event event = normalizer.normalize(tm, NYC).orElseThrow();

            assertThat(event.getVenue()).isNull();
            assertThat(event.getAddress()).isNull();
            assertThat(event.getCity()).isEqualTo("New York");
        }

        @Test
        @DisplayName("address skips blank parts")
        void partialAddress() {
            TmEvent tm = base();
            venue(tm).setAddress(null);
            venue(tm).setPostalCode(" ");

            assertThat(normalizer.normalize(tm, NYC).orElseThrow().getAddress()).isEqualTo("Brooklyn, NY");
        }

        @Test
        @DisplayName("venue with nothing but a name: address null, city from SupportedCity")
        void bareVenue() {
            TmEvent tm = base();
            tm.setEmbedded(TmEventEmbedded.builder().venues(List.of(TmVenue.builder().name("Pier 17").build())).build());

            Event event = normalizer.normalize(tm, NYC).orElseThrow();

            assertThat(event.getVenue()).isEqualTo("Pier 17");
            assertThat(event.getAddress()).isNull();
            assertThat(event.getCity()).isEqualTo("New York");
        }
    }

    @Nested
    @DisplayName("normalize: description")
    class Description {

        @Test
        @DisplayName("falls back from blank description to info, then pleaseNote")
        void fallback() {
            TmEvent tm = base();
            tm.setDescription("<p> </p>");
            tm.setInfo(null);
            tm.setPleaseNote("Bring ID.");

            assertThat(normalizer.normalize(tm, NYC).orElseThrow().getDescription()).isEqualTo("Bring ID.");
        }

        @Test
        @DisplayName("cut at 1000 chars on a word boundary")
        void truncates() {
            TmEvent tm = base();
            tm.setDescription("word ".repeat(300));

            String description = normalizer.normalize(tm, NYC).orElseThrow().getDescription();

            assertThat(description).hasSizeLessThanOrEqualTo(1000).endsWith("word").doesNotContain("  ");
        }

        @Test
        @DisplayName("all blank: null")
        void allBlank() {
            TmEvent tm = base();
            tm.setDescription("  ");
            tm.setInfo("<br/>");
            tm.setPleaseNote(null);

            assertThat(normalizer.normalize(tm, NYC).orElseThrow().getDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("normalize: image")
    class Image {

        @Test
        @DisplayName("widest 16_9 image")
        void widest169() {
            Event event = withImages(image("4_3", 2048, "https://img/4_3.jpg"), image("16_9", 640, "https://img/small.jpg"),
                    image("16_9", 1024, "https://img/big.jpg"));

            assertThat(event.getImageUrl()).isEqualTo("https://img/big.jpg");
        }

        @Test
        @DisplayName("no 16_9: widest image")
        void widestAny() {
            Event event = withImages(image("3_2", 640, "https://img/a.jpg"), image("4_3", 1200, "https://img/b.jpg"),
                    image(null, null, "https://img/c.jpg"));

            assertThat(event.getImageUrl()).isEqualTo("https://img/b.jpg");
        }

        @Test
        @DisplayName("no usable image: null")
        void none() {
            assertThat(withImages(image("16_9", 1024, " ")).getImageUrl()).isNull();
            assertThat(withImages().getImageUrl()).isNull();
        }

        private Event withImages(TmImage... images) {
            TmEvent tm = base();
            tm.setImages(new ArrayList<>(List.of(images)));
            return normalizer.normalize(tm, NYC).orElseThrow();
        }

        private TmImage image(String ratio, Integer width, String url) {
            return TmImage.builder().ratio(ratio).width(width).url(url).build();
        }
    }

    @Nested
    @DisplayName("normalize: category and interests")
    class CategoryAndInterests {

        @Test
        @DisplayName("primary classification segment, not the first one")
        void primarySegment() {
            TmEvent tm = base();
            tm.setClassifications(List.of(
                    classification(false, "Sports"),
                    classification(true, "Arts & Theatre")));

            assertThat(normalizer.normalize(tm, NYC).orElseThrow().getCategory()).isEqualTo("Arts & Theatre");
        }

        @Test
        @DisplayName("no classification or Undefined segment: Other")
        void other() {
            TmEvent tm = base();
            tm.setClassifications(null);
            assertThat(normalizer.normalize(tm, NYC).orElseThrow().getCategory()).isEqualTo("Other");

            tm.setClassifications(List.of(classification(true, "Undefined")));
            assertThat(normalizer.normalize(tm, NYC).orElseThrow().getCategory()).isEqualTo("Other");
        }

        @Test
        @DisplayName("no interests resolved: empty list")
        void noInterests() {
            lenient().when(interestMappingService.resolve(any())).thenReturn(List.of());

            assertThat(normalizer.normalize(base(), NYC).orElseThrow().getInterests()).isEmpty();
        }

        private TmClassification classification(boolean primary, String segment) {
            return TmClassification.builder().primary(primary).segment(TmNamed.builder().name(segment).build()).build();
        }
    }

    /**
     * A minimal valid event; each test changes what it needs.
     */
    private static TmEvent base() {
        TmVenue venue = TmVenue.builder()
                .name("Brooklyn Steel")
                .address(TmAddress.builder().line1("319 Frost St").build())
                .city(TmNamed.builder().name("Brooklyn").build())
                .state(TmState.builder().stateCode("NY").build())
                .postalCode("11211")
                .timezone("America/New_York")
                .build();
        return TmEvent.builder()
                .id("ev1")
                .name("Indie Night")
                .url("https://www.ticketmaster.com/event/ev1")
                .dates(TmDates.builder()
                        .start(TmStart.builder()
                                .localDate("2026-11-20").localTime("20:00:00").dateTime("2026-11-21T01:00:00Z")
                                .dateTBD(false).timeTBA(false).noSpecificTime(false)
                                .build())
                        .timezone("America/New_York")
                        .status(TmStatus.builder().code("onsale").build())
                        .build())
                .classifications(List.of(TmClassification.builder().primary(true)
                        .segment(TmNamed.builder().name("Music").build()).build()))
                .embedded(TmEventEmbedded.builder().venues(List.of(venue)).build())
                .build();
    }

    private static TmVenue venue(TmEvent tm) {
        return tm.getEmbedded().getVenues().get(0);
    }

    private static List<TmEvent> searchFixture() {
        return fixture("search-nyc-music.json", TmSearchResponse.class).events();
    }

    private static <T> T fixture(String name, Class<T> type) {
        try {
            String json = new ClassPathResource("ticketmaster/" + name).getContentAsString(StandardCharsets.UTF_8);
            return JSON.readValue(json, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
