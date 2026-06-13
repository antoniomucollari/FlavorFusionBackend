package com.toni.FoodApp.restaurant.repository;

import com.toni.FoodApp.restaurant.entity.OpeningHour;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpeningHoursRepository extends JpaRepository<OpeningHour, Long> {

    void deleteByBranch(RestaurantBranch branch);
}
