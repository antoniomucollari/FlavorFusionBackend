package com.toni.FoodApp.location.provider;


import com.google.maps.model.LatLng;
import com.toni.FoodApp.location.dto.DistanceMatrixInfo;
import com.toni.FoodApp.location.dto.RoutingProvider;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OpenRouteDistanceMatrixService implements DistanceCalculator {

    private static final Logger log = LoggerFactory.getLogger(OpenRouteDistanceMatrixService.class);
    private final RestTemplate restTemplate;
    @Value("${ors.matrix.url}")
    String orsMatrixUrl;


    @Override
    public DistanceMatrixInfo calculate(Long restaurantBranchId, String userLocationKey, LatLng origin, LatLng destination) {
        try {
            log.info("Calling ORS On-Premise Matrix for branch {}", restaurantBranchId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);


            //ORS expects [Longitude, Latitude]
            double[] originCoords = new double[]{origin.lng, origin.lat};
            double[] destCoords = new double[]{destination.lng, destination.lat};

            OrsMatrixRequest requestPayload = new OrsMatrixRequest(
                    Arrays.asList(originCoords, destCoords),
                    List.of(0), // Index 0 is the origin
                    List.of(1), // Index 1 is the destination
                    Arrays.asList("distance", "duration")
            );

            HttpEntity<OrsMatrixRequest> requestEntity = new HttpEntity<>(requestPayload, headers);

            OrsMatrixResponse response = restTemplate.postForObject(
                    orsMatrixUrl,
                    requestEntity,
                    OrsMatrixResponse.class
            );

            if (response != null
                    && response.getDistances() != null && !response.getDistances().isEmpty()
                    && response.getDurations() != null && !response.getDurations().isEmpty()) {

                // ORS returns nested arrays.
                long distance = Math.round(response.getDistances().getFirst().getFirst());
                long duration = Math.round(response.getDurations().getFirst().getFirst());

                log.info("ORS call SUCCESS for branch {}: {} meters, {} seconds",
                        restaurantBranchId, distance, duration);

                return new DistanceMatrixInfo(distance, duration);
            } else {
                log.warn("ORS API returned empty/invalid results for branch {}.", restaurantBranchId);
            }

        } catch (Exception e) {
            log.error("Exception during ORS Matrix API call for branch {}", restaurantBranchId, e);
        }

        // Fallback
        log.warn("Returning fallback -1 for branch {} via ORS", restaurantBranchId);
        return new DistanceMatrixInfo(-1L, -1L);
    }

    @Override
    public RoutingProvider getProviderType() {
        return RoutingProvider.STANDARD; // Map this to your Enum
    }


        private record OrsMatrixRequest(
                List<double[]> locations, List<Integer> sources, List<Integer> destinations,
                List<String> metrics)
        {}

    @Data
    private static class OrsMatrixResponse {
        private List<List<Double>> durations;
        private List<List<Double>> distances;
    }

    @Override
    public Map<Long, DistanceMatrixInfo> calculateBulk(List<RestaurantBranch> branches, LatLng userLocation) {
        Map<Long, DistanceMatrixInfo> results = new HashMap<>();

        if (branches == null || branches.isEmpty()) {
            return results;
        }

        try {
            log.info("Calling ORS On-Premise Bulk Matrix for {} branches", branches.size());

            List<double[]> locations = new ArrayList<>();
            List<Integer> sources = new ArrayList<>();
            List<Integer> destinations = new ArrayList<>();

            locations.add(new double[]{userLocation.lng, userLocation.lat});
            sources.add(0);

            for (int i = 0; i < branches.size(); i++) {
                RestaurantBranch branch = branches.get(i);
                locations.add(new double[]{branch.getLocation().getX(), branch.getLocation().getY()});
                destinations.add(i + 1);
            }

            // 2. Build the Request Entity
            OrsMatrixRequest requestPayload = new OrsMatrixRequest(
                    locations,
                    sources,
                    destinations,
                    Arrays.asList("distance", "duration")
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<OrsMatrixRequest> requestEntity = new HttpEntity<>(requestPayload, headers);

            OrsMatrixResponse response = restTemplate.postForObject(
                    orsMatrixUrl,
                    requestEntity,
                    OrsMatrixResponse.class
            );


            if (response != null
                    && response.getDurations() != null && !response.getDurations().isEmpty()
                    && response.getDistances() != null && !response.getDistances().isEmpty()) {

                // ORS returns an outer list representing the sources (we only have 1 source: the user)
                List<Double> durationsFromUser = response.getDurations().get(0);
                List<Double> distancesFromUser = response.getDistances().get(0);

                for (int i = 0; i < branches.size(); i++) {
                    long branchId = branches.get(i).getId();

                    Double durationVal = durationsFromUser.get(i);
                    Double distanceVal = distancesFromUser.get(i);

                    long duration = (durationVal != null) ? Math.round(durationVal) : -1L;
                    long distance = (distanceVal != null) ? Math.round(distanceVal) : -1L;

                    results.put(branchId, new DistanceMatrixInfo(distance, duration));
                }

                log.info("Successfully calculated bulk routing for {} branches.", branches.size());
            } else {
                log.warn("ORS Bulk API returned empty or invalid results.");
            }

        } catch (Exception e) {
            log.error("Exception during ORS Bulk Matrix API call", e);
            for (RestaurantBranch branch : branches) {
                results.put(branch.getId(), new DistanceMatrixInfo(-1L, -1L));
            }
        }

        return results;
    }
}