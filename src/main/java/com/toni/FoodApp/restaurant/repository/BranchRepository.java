package com.toni.FoodApp.restaurant.repository;

import com.toni.FoodApp.restaurant.entity.BranchMenuItem;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<RestaurantBranch, Long>, JpaSpecificationExecutor<RestaurantBranch> {
    List<RestaurantBranch> findByRestaurantId(Long restaurantId);
    @Query("SELECT bmi FROM BranchMenuItem bmi " +
            "JOIN FETCH bmi.menu m " +
            "JOIN FETCH m.category c " +
            "WHERE bmi.branch.id = :branchId " +
            "AND bmi.isAvailable = true " +
            "AND c.deleted = false " +
            "AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :searchString, '%')) " +
            "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :searchString, '%')))")
    List<BranchMenuItem> findAllWithDetailsAndSearch(
            @Param("branchId") Long branchId,
            @Param("searchString") String searchString);

    Optional<RestaurantBranch> findByManagerId(Long userId);

    @Query("SELECT o.branch FROM Order o WHERE o.id = :orderId")
    Optional<RestaurantBranch> findByOrderId(@Param("orderId") Long orderId);
}
