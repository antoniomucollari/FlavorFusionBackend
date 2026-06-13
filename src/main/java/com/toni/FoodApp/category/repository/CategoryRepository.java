package com.toni.FoodApp.category.repository;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByRestaurantIdOrderByIdAsc(Long restaurantId);
}
