package com.toni.FoodApp.restaurant.service;

import com.google.maps.model.LatLng;
import com.toni.FoodApp.deliverTo.dtos.DeliveryInfo;
import com.toni.FoodApp.location.dto.DistanceMatrixInfo;
import com.toni.FoodApp.location.dto.RoutingProvider;
import com.toni.FoodApp.location.service.DistanceRoutingManager;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PricingService {
    private final DistanceRoutingManager distanceRoutingManager;
    private static final long MIN_BUFFER_SECONDS = 5 * 60;
    private static final long MAX_BUFFER_SECONDS = 10 * 60;

    public String calculateDeliveryTime(int avgPrepTimeInMinutes, long travelTimeInSeconds) {
        if (travelTimeInSeconds == -1) {
            return "N/A"; // Handle API error
        }

        long minTotalSeconds = (avgPrepTimeInMinutes * 60L) + travelTimeInSeconds + MIN_BUFFER_SECONDS;
        long maxTotalSeconds = (avgPrepTimeInMinutes * 60L) + travelTimeInSeconds + MAX_BUFFER_SECONDS;

        // Convert to minutes
        long minTotalMinutes = minTotalSeconds / 60;
        long maxTotalMinutes = maxTotalSeconds / 60;

        // Round to nearest 5 minutes
        long roundedMin = roundToNearest5(minTotalMinutes);
        long roundedMax = roundToNearest5(maxTotalMinutes);

        return roundedMin + "-" + roundedMax + " min";
    }

    private long roundToNearest5(long minutes) {
        return (long) (Math.round(minutes / 5.0) * 5);
    }



    public BigDecimal calculateDeliveryPrice(long distanceInMeters) {
        if (distanceInMeters == -1) {
            return BigDecimal.valueOf(-1); // Handle API error
        }

        double distanceInKm = distanceInMeters / 1000.0;

        // Example price tiers (adjust as needed)
        if (distanceInKm <= 1.5) {
            return new BigDecimal("80");
        } else if (distanceInKm <= 3.0) {
            return new BigDecimal("150");
        } else if (distanceInKm <= 5.0) {
            return new BigDecimal("200");
        } else {
            // Handle cases over 5km (maybe they are filtered out)
            return new BigDecimal("300");
        }
    }


    public DeliveryInfo calculateDeliveryInfo(RestaurantBranch branch, double userLatitude, double userLongitude, RoutingProvider routingProvider) {

        LatLng origin = new LatLng(branch.getLocation().getY(), branch.getLocation().getX());
        LatLng destination = new LatLng(userLatitude, userLongitude);
        String userLocationKey = getUserLocationCacheKey(userLatitude, userLongitude);

        DistanceMatrixInfo matrixInfo = distanceRoutingManager.getDistanceMatrix(
                branch.getId(), userLocationKey, origin, destination, routingProvider
        );

        boolean isOpen = !branch.isClosed();
        boolean routeExists = matrixInfo.getDistanceInMeters() != -1;
        double roadDistanceKm = matrixInfo.getDistanceInMeters() / 1000.0;
        boolean inRadius = roadDistanceKm <= branch.getDeliveryRadiusInKm();

        boolean isDeliverable = isOpen && routeExists && inRadius;

        BigDecimal deliveryPrice = BigDecimal.ZERO;
        String deliveryTime = "N/A";

        if (isDeliverable) {
            deliveryPrice = calculateDeliveryPrice(matrixInfo.getDistanceInMeters());
            deliveryTime = calculateDeliveryTime(
                    branch.getAvgPrepTimeInMinutes(),
                    matrixInfo.getDurationInSeconds()
            );
        }

        return DeliveryInfo.builder()
                .isDeliverable(isDeliverable)
                .deliveryFee(deliveryPrice)
                .deliveryTime(deliveryTime)
                .distanceInKm(Math.round(roadDistanceKm * 100.0) / 100.0) // Round it here
                .distanceInMeters(matrixInfo.getDistanceInMeters())
                .durationInSeconds(matrixInfo.getDurationInSeconds())
                .build();
    }
    public Map<Long, DeliveryInfo> calculateDeliveryInfoBulk(
            List<RestaurantBranch> branches,
            double userLat,
            double userLng,
            RoutingProvider routingProvider) {


        Map<Long, DistanceMatrixInfo> matrixMap = distanceRoutingManager.getDistanceMatrixBulk(
                branches, userLat, userLng, routingProvider
        );

        Map<Long, DeliveryInfo> deliveryInfoMap = new HashMap<>();


        for (RestaurantBranch branch : branches) {
            DistanceMatrixInfo matrixInfo = matrixMap.getOrDefault(branch.getId(), new DistanceMatrixInfo(-1L, -1L));

            boolean isOpen = !branch.isClosed();
            boolean routeExists = matrixInfo.getDistanceInMeters() != -1;
            double roadDistanceKm = matrixInfo.getDistanceInMeters() / 1000.0;
            boolean inRadius = roadDistanceKm <= branch.getDeliveryRadiusInKm();

            boolean isDeliverable = isOpen && routeExists && inRadius;

            BigDecimal deliveryPrice = BigDecimal.ZERO;
            String deliveryTime = "N/A";

            if (isDeliverable) {
                deliveryPrice = calculateDeliveryPrice(matrixInfo.getDistanceInMeters());
                deliveryTime = calculateDeliveryTime(branch.getAvgPrepTimeInMinutes(), matrixInfo.getDurationInSeconds());
            }

            DeliveryInfo info = DeliveryInfo.builder()
                    .isDeliverable(isDeliverable)
                    .deliveryFee(deliveryPrice)
                    .deliveryTime(deliveryTime)
                    .distanceInKm(Math.round(roadDistanceKm * 100.0) / 100.0)
                    .distanceInMeters(matrixInfo.getDistanceInMeters())
                    .durationInSeconds(matrixInfo.getDurationInSeconds())
                    .build();

            deliveryInfoMap.put(branch.getId(), info);
        }

        return deliveryInfoMap;
    }
    // cache ~400m radius
    private String getUserLocationCacheKey(double lat, double lon) {
        int precision = 250;
        double latKey = Math.floor(lat * precision) / precision;
        double lonKey = Math.floor(lon * precision) / precision;
        return String.format("%.4f,%.4f", latKey, lonKey);
    }


}