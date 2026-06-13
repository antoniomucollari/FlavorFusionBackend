package com.toni.FoodApp.cart.repository;

import com.toni.FoodApp.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserIdAndRestaurantBranchId(Long userId, Long branchId);


}
