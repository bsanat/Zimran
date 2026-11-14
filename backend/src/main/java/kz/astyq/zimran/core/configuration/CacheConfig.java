package kz.astyq.zimran.core.configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Caffeine caches. Only the caches named here exist, each with its own TTL and maximum size.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Normalized events of one Ticketmaster search, keyed by {@code EventSearchKey}. */
    public static final String EVENT_SEARCH = "eventSearch";
    /** Same entries as {@link #EVENT_SEARCH}, kept longer; read only when Ticketmaster is unavailable. */
    public static final String EVENT_SEARCH_STALE = "eventSearchStale";
    /** One normalized event, keyed by {@code EventId}. */
    public static final String EVENT_BY_ID = "eventById";

    @Bean
    public CacheManager cacheManager(AppCacheProperties properties) {
        return buildCacheManager(properties);
    }

    /**
     * The cache manager without Spring, so tests can use the real caches.
     */
    public static CaffeineCacheManager buildCacheManager(AppCacheProperties properties) {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setAllowNullValues(false);
        // registerCustomCache switches the manager to static mode: unknown cache names return null.
        manager.registerCustomCache(EVENT_SEARCH, cache(properties.getSearchTtl(), properties.getSearchMaxSize()));
        manager.registerCustomCache(EVENT_SEARCH_STALE, cache(properties.getStaleTtl(), properties.getStaleMaxSize()));
        manager.registerCustomCache(EVENT_BY_ID, cache(properties.getByIdTtl(), properties.getByIdMaxSize()));
        return manager;
    }

    private static Cache<Object, Object> cache(Duration ttl, long maxSize) {
        return Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(maxSize).build();
    }
}
