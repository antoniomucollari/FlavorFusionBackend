package com.toni.FoodApp.restaurant.service;

import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.dtos.*;
import com.toni.FoodApp.restaurant.dtos.response.RestaurantSummaryDTO;
import com.toni.FoodApp.restaurant.filterAndSpecification.BranchFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface RestaurantService {

    Response<?> createRestaurant(RestaurantRequestDto restaurantDTO);

    Response<Page<RestaurantSummaryDTO>> findAll(Pageable pageable);

    Page<RestaurantSummaryDTO> findAvailableRestaurants(RestaurantFilterCriteria criteria, Pageable pageable,String sort, Double lat, Double lng);


    Response<List<SimpleBranchDto>> findBranchesForRestaurant(BranchFilter filter);

    Response<SimpleRestaurantDto> getCurrentRestaurant();


    Response<Page<RestaurantsDto>> findAllForAdmin(Pageable pageable,Boolean deleted);

    Response<?> deleteRestaurant(Long restaurantId);

    Response<?> unassignRestaurant(Long restaurantId);

    Response<?> restoreRestaurant(Long restaurantId);

    Response<RestaurantSummaryDTO> findByRestaurant(Long id);
}
