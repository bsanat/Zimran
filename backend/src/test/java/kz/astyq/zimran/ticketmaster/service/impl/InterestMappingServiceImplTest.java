package kz.astyq.zimran.ticketmaster.service.impl;

import kz.astyq.zimran.core.configuration.InterestMappingProperties;
import kz.astyq.zimran.core.configuration.InterestMappingProperties.ClassificationRule;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.ticketmaster.model.dto.TmClassification;
import kz.astyq.zimran.ticketmaster.model.dto.TmNamed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against the rules in the real {@code application.yml}, so the configured mapping is tested too.
 */
class InterestMappingServiceImplTest {

    private InterestMappingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InterestMappingServiceImpl(applicationYmlMapping());
    }

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @ParameterizedTest(name = "{0} / {1} / {2} -> {3}")
        @CsvSource(nullValues = "-", value = {
                "Music,           Rock,                         -,       MUSIC",
                "Sports,          Basketball,                   NBA,     SPORTS",
                "Arts & Theatre,  Theatre,                      Drama,   THEATER",
                "Arts & Theatre,  Musical,                      -,       THEATER",
                "Arts & Theatre,  Opera,                        -,       THEATER",
                "Arts & Theatre,  Dance,                        Ballet,  THEATER",
                "Arts & Theatre,  Fine Art,                     -,       ART",
                "Arts & Theatre,  Multimedia,                   -,       ART",
                "Arts & Theatre,  Cultural,                     -,       ART",
                "Arts & Theatre,  Comedy,                       Comedy,  COMEDY",
                "Miscellaneous,   Food & Drink,                 -,       FOOD",
                "Miscellaneous,   Community/Civic,              Club,    NIGHTLIFE",
                "Music,           Dance/Electronic,             -,       NIGHTLIFE",
                "Miscellaneous,   Fairs & Festivals,            -,       OUTDOORS",
                "Miscellaneous,   Outdoor,                      -,       OUTDOORS",
                "Miscellaneous,   Lecture/Seminar,              -,       TECHNOLOGY",
                "Sports,          Esports,                      -,       GAMING",
                "Miscellaneous,   Hobby/Special Interest Expos, Gaming,  GAMING"
        })
        @DisplayName("every configured rule matches its classification")
        void everyRule(String segment, String genre, String subGenre, Interest expected) {
            assertThat(service.resolve(List.of(classification(segment, genre, subGenre)))).contains(expected);
        }

        @Test
        @DisplayName("Dance/Electronic music is both MUSIC and NIGHTLIFE")
        void twoInterestsFromOneClassification() {
            assertThat(service.resolve(List.of(classification("Music", "Dance/Electronic", "House"))))
                    .containsExactly(Interest.MUSIC, Interest.NIGHTLIFE);
        }

        @Test
        @DisplayName("matches against all classifications, not only the primary one")
        void allClassifications() {
            assertThat(service.resolve(List.of(
                    classification("Sports", "Esports", null),
                    classification("Arts & Theatre", "Comedy", null))))
                    .containsExactly(Interest.SPORTS, Interest.COMEDY, Interest.GAMING);
        }

        @Test
        @DisplayName("case-insensitive and trims names")
        void caseInsensitive() {
            assertThat(service.resolve(List.of(classification(" arts & THEATRE ", "fine art", null))))
                    .containsExactly(Interest.ART);
        }

        @Test
        @DisplayName("genre-restricted rule does not match the bare segment")
        void segmentAloneIsNotEnough() {
            assertThat(service.resolve(List.of(classification("Arts & Theatre", "Puppetry", null)))).isEmpty();
        }

        @Test
        @DisplayName("null, empty, null entries and nameless classifications: no interests")
        void nothing() {
            assertThat(service.resolve(null)).isEmpty();
            assertThat(service.resolve(List.of())).isEmpty();
            List<TmClassification> withNull = new ArrayList<>();
            withNull.add(null);
            withNull.add(TmClassification.builder().build());
            assertThat(service.resolve(withNull)).isEmpty();
        }
    }

    @Nested
    @DisplayName("classificationNamesFor")
    class ClassificationNamesFor {

        @Test
        @DisplayName("most specific name of each rule, without duplicates")
        void names() {
            assertThat(service.classificationNamesFor(Interest.MUSIC)).containsExactly("Music");
            assertThat(service.classificationNamesFor(Interest.THEATER))
                    .containsExactly("Theatre", "Musical", "Opera", "Dance");
            assertThat(service.classificationNamesFor(Interest.GAMING)).containsExactly("Esports", "Gaming");
            assertThat(service.classificationNamesFor(Interest.NIGHTLIFE)).containsExactly("Club", "Dance/Electronic");
        }

        @Test
        @DisplayName("interest without coverage: empty")
        void noCoverage() {
            InterestMappingServiceImpl partial = new InterestMappingServiceImpl(mapping(Map.of(
                    Interest.MUSIC, List.of(ClassificationRule.builder().segment("Music").build()))));

            assertThat(partial.classificationNamesFor(Interest.FOOD)).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasCoverage")
    class HasCoverage {

        @Test
        @DisplayName("every interest is covered by application.yml")
        void allCovered() {
            assertThat(Arrays.stream(Interest.values()).filter(service::hasCoverage)).containsExactly(Interest.values());
        }

        @Test
        @DisplayName("interest without rules: no coverage and never resolved")
        void noCoverage() {
            InterestMappingServiceImpl partial = new InterestMappingServiceImpl(mapping(Map.of(
                    Interest.MUSIC, List.of(ClassificationRule.builder().segment("Music").build()),
                    Interest.FOOD, List.of())));

            assertThat(partial.hasCoverage(Interest.MUSIC)).isTrue();
            assertThat(partial.hasCoverage(Interest.FOOD)).isFalse();
            assertThat(partial.hasCoverage(Interest.COMEDY)).isFalse();
            assertThat(partial.resolve(List.of(classification("Miscellaneous", "Food & Drink", null)))).isEmpty();
        }
    }

    private static TmClassification classification(String segment, String genre, String subGenre) {
        return TmClassification.builder()
                .segment(named(segment))
                .genre(named(genre))
                .subGenre(named(subGenre))
                .build();
    }

    private static TmNamed named(String name) {
        return name != null ? TmNamed.builder().name(name).build() : null;
    }

    private static InterestMappingProperties mapping(Map<Interest, List<ClassificationRule>> rules) {
        InterestMappingProperties properties = new InterestMappingProperties();
        properties.setInterestMapping(new LinkedHashMap<>(rules));
        return properties;
    }

    private static InterestMappingProperties applicationYmlMapping() {
        try {
            List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                    .load("application.yml", new ClassPathResource("application.yml"));
            StandardEnvironment environment = new StandardEnvironment();
            sources.forEach(environment.getPropertySources()::addLast);
            return new Binder(ConfigurationPropertySources.get(environment))
                    .bind("app.ticketmaster", InterestMappingProperties.class)
                    .orElseThrow(() -> new IllegalStateException("app.ticketmaster.interest-mapping not bound"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
