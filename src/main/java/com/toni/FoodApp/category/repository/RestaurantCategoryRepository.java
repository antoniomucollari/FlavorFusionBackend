package com.toni.FoodApp.category.repository;

import com.toni.FoodApp.category.entity.Category;
import com.toni.FoodApp.category.entity.RestaurantCategory;
import com.toni.FoodApp.restaurant.repository.RestaurantRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RestaurantCategoryRepository extends JpaRepository<RestaurantCategory, Long> {
    @Query("SELECT r.categories FROM Restaurant r WHERE r.id = :restaurantId")
    List<RestaurantCategory> findCategoriesByRestaurantId(Long restaurantId);
}
