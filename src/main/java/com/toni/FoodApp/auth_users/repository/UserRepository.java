package com.toni.FoodApp.auth_users.repository;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN u.roles r " +
            "WHERE r.name = :roleName " +
            "AND (" +
            "   :search IS NULL OR " +
            "   LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))" +
            ") " +
            "ORDER BY u.id DESC")
    List<User> findAllByRoleNameAndSearch(
            @Param("roleName") RoleName roleName,
            @Param("search") String search
    );

//    @Query("SELECT u FROM User u JOIN RestaurantBranch rl  ON u.id = rl.manager.id " +
//            "JOIN u.roles r WHERE rl.restaurant.id = :restaurantId and r.name = 'BRANCH_MANAGER' and u.createdByCompany = :restaurantId")
//    List<User> findBranchManagersForRestaurant(@Param("restaurantId") Long restaurantId);
//
//
    @Query("Select rb.restaurant.id from RestaurantBranch rb where rb.id = :branchId")
    Optional<Long> findRestaurantIdByBranchId(@Param("userId") Long branchId);
}
