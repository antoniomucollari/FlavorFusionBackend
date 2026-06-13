package com.toni.FoodApp.restaurant.repository;

import com.toni.FoodApp.restaurant.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ReviewRepository  extends JpaRepository<Review, Long> {
    List<Review> findByBranchIdOrderByIdDesc(Long restaurantId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.branch.id = :branchId")
    Double calculateAverageRatingByRestaurantId(@Param("branchId") Long branchId);

    // Fix 2: Changed to check r.order.id and use :orderId
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Review r " +
            "WHERE r.user.id = :userId AND r.order.id = :orderId")
    boolean existsByUserIdAndOrderId(
            @Param("userId") Long userId,
            @Param("orderId") Long orderId
    );
    //* COALESCE is used to return 0.0 if there are no reviews, instead of null.
    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.branch.id = :branchId")
    double getAverageRatingByBranchId(@Param("branchId") Long branchId);

    Long countByBranchId(Long branchId);

    @Query("Select COUNT(r) > 0 from Review r where r.order.id = :orderId and r.user.id = :userId ")
    Boolean orderExistsInReview(@Param("orderId") Long orderId, @Param("userId" ) Long userId);


    @Query("Select r from Review r where r.order.id = :id")
    Optional<Review> findByOrderId(Long id);
}
