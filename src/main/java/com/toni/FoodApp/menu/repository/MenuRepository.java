package com.toni.FoodApp.menu.repository;

import com.toni.FoodApp.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long>, JpaSpecificationExecutor<Menu> {
    @Query("SELECT m FROM Menu m " +
            "WHERE m.category.restaurant.id = :restaurantId " +
            "AND m.id NOT IN (" +
            "    SELECT bmi.menu.id " +
            "    FROM BranchMenuItem bmi " +
            "    WHERE bmi.branch.id = :branchId" +
            ")")
    List<Menu> findMenusNotLinkedToBranch(
            @Param("restaurantId") Long restaurantId,
            @Param("branchId") Long branchId
    );

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END " +
            "FROM Menu m " +
            "JOIN m.category c " +
            "JOIN c.restaurant r " +
            "WHERE m.id = :menuId AND r.owner.id = :ownerId")
    boolean existsByIdAndOwnerId(Long menuId, Long ownerId);
}
