package com.toni.FoodApp.location.provider;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import com.google.maps.model.Unit;
import com.toni.FoodApp.location.dto.DistanceMatrixInfo;
import com.toni.FoodApp.location.dto.RoutingProvider;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class GoogleDistanceMatrixService implements DistanceCalculator {

    private final GeoApiContext geoApiContext;
    @Override
    public DistanceMatrixInfo calculate(Long restaurantBranchId, String userLocationKey, LatLng origin, LatLng destination) {

        try {
            log.info("Calling Distance Matrix API for branch {} from {} to {}",
                    restaurantBranchId, origin.toString(), destination.toString());

            DistanceMatrix matrix = DistanceMatrixApi.newRequest(geoApiContext)
                    .origins(origin)
                    .destinations(destination)
                    .mode(TravelMode.DRIVING)
                    .units(Unit.METRIC)
                    .await();

            // Check for a valid result
            if (matrix.rows.length > 0 && matrix.rows[0].elements.length > 0) {
                var element = matrix.rows[0].elements[0];

                // --- THIS IS THE NEW DEBUGGING PART ---
                if ("OK".equals(element.status.name())) {
                    log.info("API call SUCCESS for branch {}: {} meters, {} seconds",
                            restaurantBranchId, element.distance.inMeters, element.duration.inSeconds);

                    return new DistanceMatrixInfo(
                            element.distance.inMeters,
                            element.duration.inSeconds
                    );
                } else {
                    log.error("API call FAILED for branch {}. Status: {}",
                            restaurantBranchId, element.status.name());
                }

            } else {
                log.warn("API call returned empty/invalid results for branch {}.", restaurantBranchId);
            }

        } catch (Exception e) {
            log.error("Exception during Distance Matrix API call for branch " + restaurantBranchId, e);
            throw new RuntimeException("Distance calculation failed", e);
        }

        log.warn("Returning fallback -1 for branch {}", restaurantBranchId);
        return new DistanceMatrixInfo(-1L, -1L);
    }

    @Override
    public Map<Long, DistanceMatrixInfo> calculateBulk(List<RestaurantBranch> branches, LatLng userLocation) {
        return Map.of();
    }


    @Override
    public RoutingProvider getProviderType() {
        return RoutingProvider.HIGH_ACCURACY;
    }


}

