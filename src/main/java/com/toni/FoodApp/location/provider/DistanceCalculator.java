package com.toni.FoodApp.location.provider;

import com.google.maps.model.LatLng;
import com.toni.FoodApp.location.dto.DistanceMatrixInfo;
import com.toni.FoodApp.location.dto.RoutingProvider;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;

import java.util.List;
import java.util.Map;

public interface DistanceCalculator {
    DistanceMatrixInfo calculate(Long restaurantBranchId, String userLocationKey, LatLng origin, LatLng destination);
    Map<Long, DistanceMatrixInfo> calculateBulk(List<RestaurantBranch> branches, LatLng userLocation);


    RoutingProvider getProviderType();
}