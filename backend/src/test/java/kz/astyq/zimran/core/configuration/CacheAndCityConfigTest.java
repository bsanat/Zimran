package kz.astyq.zimran.core.configuration;

import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.city.service.impl.SupportedCityServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Binds the real {@code application.yml} into the cache and city settings and starts their beans (no database).
 */
class CacheAndCityConfigTest {

    @Configuration
    @EnableConfigurationProperties({AppCacheProperties.class, CityProperties.class})
    static class Properties {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(Properties.class, CacheConfig.class, ClockConfig.class,
                    SupportedCityServiceImpl.class);

    @Test
    @DisplayName("application.yml: NYC is the default city, caches use the documented TTLs, clock is UTC")
    void applicationYml() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            SupportedCityService cities = context.getBean(SupportedCityService.class);
            assertThat(cities.getDefault().getName()).isEqualTo("New York");
            assertThat(cities.getDefault().getZone()).isEqualTo(ZoneId.of("America/New_York"));

            AppCacheProperties cache = context.getBean(AppCacheProperties.class);
            assertThat(cache.getSearchTtl()).isEqualTo(Duration.ofMinutes(10));
            assertThat(cache.getStaleTtl()).isEqualTo(Duration.ofHours(6));
            assertThat(cache.getByIdTtl()).isEqualTo(Duration.ofMinutes(30));
            assertThat(cache.getByIdMaxSize()).isEqualTo(5000);
            assertThat(context.getBean(CacheManager.class).getCacheNames()).containsExactlyInAnyOrder(
                    CacheConfig.EVENT_SEARCH, CacheConfig.EVENT_SEARCH_STALE, CacheConfig.EVENT_BY_ID);
            assertThat(context.getBean(Clock.class).getZone()).isEqualTo(java.time.ZoneOffset.UTC);
        });
    }

    @Test
    @DisplayName("default city that is not supported: startup fails")
    void unknownDefaultCity() {
        runner.withPropertyValues("app.city.default-id=sf").run(context -> assertThat(context).hasFailed());
    }
}
