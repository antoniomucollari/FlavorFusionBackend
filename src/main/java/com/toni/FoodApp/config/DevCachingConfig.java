package com.toni.FoodApp.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.NoOpCache;
import org.springframework.context.annotation.Configuration;

import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Profile;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Configuration
@Profile("dev")
public class DevCachingConfig {

    @Bean
    public CacheManager cacheManager() {
        Set<String> allowedCaches = Set.of("distanceMatrix");

        Map<String, Cache> caches = new HashMap<>();
        caches.put("distanceMatrix", new ConcurrentMapCache("distanceMatrix"));

        return new CacheManager() {
            @Override
            public Cache getCache(@NotNull String name) {
                if (allowedCaches.contains(name)) {
                    return caches.computeIfAbsent(name, ConcurrentMapCache::new);
                }
                // No-op: returns null for get, does nothing for put/evict
                return new NoOpCache(name);
            }

            @NotNull
            @Override
            public Collection<String> getCacheNames() {
                return allowedCaches;
            }
        };
    }
}