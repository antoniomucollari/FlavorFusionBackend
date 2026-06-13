package com.toni.FoodApp.location.service;

import com.google.maps.model.LatLng;
import com.toni.FoodApp.location.dto.DistanceMatrixInfo;
import com.toni.FoodApp.location.provider.DistanceCalculator;
import com.toni.FoodApp.location.dto.RoutingProvider;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DistanceRoutingManager {

    private final Map<RoutingProvider, DistanceCalculator> calculatorMap;
    private final CacheManager cacheManager; // Inject the cache manager!

    public DistanceRoutingManager(List<DistanceCalculator> calculators, CacheManager cacheManager) {
        this.calculatorMap = calculators.stream()
                .collect(Collectors.toMap(DistanceCalculator::getProviderType, calc -> calc));
        this.cacheManager = cacheManager;
    }
    @org.springframework.cache.annotation.Cacheable(
            value = "distanceMatrix",
            key = "'branch:' + #restaurantBranchId + ':loc:' + #userLocationKey + ':provider:' + #provider.name()"
    )
    public DistanceMatrixInfo getDistanceMatrix(
            Long restaurantBranchId,
            String userLocationKey,
            LatLng origin,
            LatLng destination,
            RoutingProvider provider) {

        DistanceCalculator calculator = calculatorMap.get(provider);

        if (calculator == null) {
            log.error("No distance calculator found for provider: {}", provider);
            return new DistanceMatrixInfo(-1L, -1L);
        }

        log.info("Routing single distance calculation to {}", provider.name());
        return calculator.calculate(restaurantBranchId, userLocationKey, origin, destination);
    }
    // THE NEW BULK METHOD
    public Map<Long, DistanceMatrixInfo> getDistanceMatrixBulk(
            List<RestaurantBranch> branches,
            double userLat,
            double userLng,
            RoutingProvider provider) {

        String userLocationKey = getUserLocationCacheKey(userLat, userLng);
        Cache cache = cacheManager.getCache("distanceMatrix");
        Map<Long, DistanceMatrixInfo> finalResults = new HashMap<>();
        List<RestaurantBranch> missingBranches = new ArrayList<>();

        // 1. Check cache for every branch
        for (RestaurantBranch branch : branches) {
            String cacheKey = "branch:" + branch.getId() + ":loc:" + userLocationKey + ":provider:" + provider.name();
            Cache.ValueWrapper cachedValue = (cache != null) ? cache.get(cacheKey) : null;

            if (cachedValue != null && cachedValue.get() != null) {
                finalResults.put(branch.getId(), (DistanceMatrixInfo) cachedValue.get());
            } else {
                missingBranches.add(branch);
            }
        }

        // 2. If there are misses, process them in ONE bulk request
        if (!missingBranches.isEmpty()) {
            log.info("Cache missed for {} branches. Sending bulk request to {}", missingBranches.size(), provider.name());

            DistanceCalculator calculator = calculatorMap.get(provider);
            LatLng userDest = new LatLng(userLat, userLng);

            // Call the bulk calculator method
            Map<Long, DistanceMatrixInfo> freshData = calculator.calculateBulk(missingBranches, userDest);

            // 3. Put fresh data into the cache and add to final results
            for (Map.Entry<Long, DistanceMatrixInfo> entry : freshData.entrySet()) {
                String cacheKey = "branch:" + entry.getKey() + ":loc:" + userLocationKey + ":provider:" + provider.name();
                if (cache != null) {
                    cache.put(cacheKey, entry.getValue());
                }
                finalResults.put(entry.getKey(), entry.getValue());
            }
        }

        return finalResults;
    }

    private String getUserLocationCacheKey(double lat, double lon) {
        int precision = 250;
        double latKey = Math.floor(lat * precision) / precision;
        double lonKey = Math.floor(lon * precision) / precision;
        return String.format("%.4f,%.4f", latKey, lonKey);
    }
}