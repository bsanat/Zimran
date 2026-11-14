package kz.astyq.zimran.jsonsource.service.impl;

import kz.astyq.zimran.core.configuration.EventSourceProperties;
import kz.astyq.zimran.event.model.enums.Interest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMappingServiceImplTest {

    private CategoryMappingServiceImpl service;

    @BeforeEach
    void setUp() {
        EventSourceProperties properties = new EventSourceProperties();
        properties.setCategoryMapping(Map.of(
                Interest.MUSIC, List.of("Concerts", "Live  Music"),
                Interest.NIGHTLIFE, List.of("concerts", "Club"),
                Interest.FOOD, List.of("Food & Drink")));
        service = new CategoryMappingServiceImpl(properties);
    }

    @Nested
    @DisplayName("interestsFor")
    class InterestsFor {

        @Test
        @DisplayName("configured names, ignoring case and extra spaces; one name may map to several interests")
        void configured() {
            assertThat(service.interestsFor(List.of("CONCERTS"))).containsExactly(Interest.MUSIC, Interest.NIGHTLIFE);
            assertThat(service.interestsFor(List.of(" live music "))).containsExactly(Interest.MUSIC);
            assertThat(service.interestsFor(List.of("food & drink"))).containsExactly(Interest.FOOD);
        }

        @Test
        @DisplayName("every interest id maps to itself without configuration")
        void directIds() {
            for (Interest interest : Interest.values()) {
                assertThat(service.interestsFor(List.of(interest.getId().toUpperCase()))).containsExactly(interest);
            }
        }

        @Test
        @DisplayName("order follows the categories, duplicates removed; unknown, blank and null ignored")
        void orderAndUnknown() {
            assertThat(service.interestsFor(Arrays.asList("Club", null, " ", "Pottery", "music", "club")))
                    .containsExactly(Interest.NIGHTLIFE, Interest.MUSIC);
            assertThat(service.interestsFor(null)).isEmpty();
        }
    }
}
