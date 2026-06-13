package com.toni.FoodApp.menu.repository;

import com.toni.FoodApp.menu.dtos.MenuOptionFlatDTO;
import com.toni.FoodApp.menu.entity.OptionGroup;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.Id;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OptionRepository extends JpaRepository<OptionGroup, Long> {

    boolean existsByIdAndRestaurantId(Long id, Long restaurantId);
    List<OptionGroup> findByRestaurantId(Long restaurantId);

    @Query("SELECT og FROM OptionGroup og " +
            "WHERE og.restaurant.id = :restaurantId " +
            "AND og.id NOT IN " +
            "(SELECT g.id FROM Menu m JOIN m.optionGroups g WHERE m.id = :menuId)")
    List<OptionGroup> findAvailableOptionsForMenu(@Param("restaurantId") Long restaurantId,
                                                            @Param("menuId") Long menuId);
    @Modifying
    @Query(value = "DELETE FROM menu_option_groups WHERE group_id = :groupId", nativeQuery = true)
    void unlinkFromAllMenus(@Param("groupId") Long groupId);

    @Query("""
    SELECT new com.toni.FoodApp.menu.dtos.MenuOptionFlatDTO(
        og.id,  
        og.name, 
        og.minSelection, 
        og.maxSelection,
        v.id, 
        v.name, 
        COALESCE(boc.priceOverride, v.recommendedPrice) 
    )
    FROM BranchMenuItem bmi
    JOIN bmi.menu m
    JOIN m.optionGroups og
    JOIN og.variants v
    LEFT JOIN BranchOptionConfig boc 
        ON boc.variant = v AND boc.branch.id = :branchId
    WHERE bmi.id = :branchMenuId
    AND (
        (boc.isAvailable IS TRUE) 
        OR 
        (boc.id IS NULL AND v.isDeleted IS FALSE)
    )
    ORDER BY og.id ASC, v.id ASC
""")
    List<MenuOptionFlatDTO> findAvailableOptionsRaw(
            @Param("branchMenuId") Long branchMenuId,
            @Param("branchId") Long branchId
    );


    @Query("""
        select og from OptionGroup og where og.id = (select ov.group.id from OptionVariant ov where ov.id = :variantId)
""")
    OptionGroup getOptionByVariantId(@Param("variantId")Long variantId);
}
