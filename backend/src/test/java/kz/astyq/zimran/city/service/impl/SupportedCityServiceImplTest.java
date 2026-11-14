package kz.astyq.zimran.city.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.core.configuration.CityProperties;
import kz.astyq.zimran.core.configuration.CityProperties.CityDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SupportedCityServiceImplTest {

    private SupportedCityServiceImpl service;

    @BeforeEach
    void setUp() {
        CityProperties properties = new CityProperties();
        properties.setDefaultId("nyc");
        properties.setSupported(List.of(
                CityDefinition.builder().id("nyc").name("New York").stateCode("NY").countryCode("US")
                        .zone(ZoneId.of("America/New_York")).build(),
                CityDefinition.builder().id("la").name("Los Angeles").stateCode("CA").countryCode("US")
                        .zone(ZoneId.of("America/Los_Angeles")).build()));
        service = new SupportedCityServiceImpl(properties);
    }

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("configured cities in order, mapped to the domain")
        void all() {
            assertThat(service.getAll()).extracting(SupportedCity::getId).containsExactly("nyc", "la");
            assertThat(service.getAll().get(0).getZone()).isEqualTo(ZoneId.of("America/New_York"));
        }
    }

    @Nested
    @DisplayName("findById / findByName")
    class Find {

        @Test
        @DisplayName("case-insensitive, trimmed; blank or unknown: empty")
        void find() {
            assertThat(service.findById(" LA ")).map(SupportedCity::getName).contains("Los Angeles");
            assertThat(service.findByName(" new york ")).map(SupportedCity::getId).contains("nyc");
            assertThat(service.findById("sf")).isEmpty();
            assertThat(service.findByName("Brooklyn")).isEmpty();
            assertThat(service.findById(null)).isEmpty();
            assertThat(service.findByName(" ")).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDefault")
    class GetDefault {

        @Test
        @DisplayName("the configured default city")
        void defaultCity() {
            assertThat(service.getDefault().getId()).isEqualTo("nyc");
        }
    }
}
