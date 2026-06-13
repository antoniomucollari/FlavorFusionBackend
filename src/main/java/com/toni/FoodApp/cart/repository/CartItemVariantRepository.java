package com.toni.FoodApp.cart.repository;

import com.toni.FoodApp.cart.dtos.VariantAvailabilityDTO;
import com.toni.FoodApp.cart.entity.CartItem;
import com.toni.FoodApp.cart.entity.CartItemVariant;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemVariantRepository extends JpaRepository<CartItemVariant, Long> {

    @Query("SELECT CASE WHEN COUNT(civ) > 0 THEN true ELSE false END " +
            "FROM CartItemVariant civ " +
            "WHERE civ.optionVariant.group.id = :groupId")
    boolean isOptionGroupInUse(@Param("groupId") Long groupId);

    List<CartItemVariant> findByCartItemId(Long cartItemId);

    @Query("""
    SELECT new com.toni.FoodApp.cart.dtos.VariantAvailabilityDTO(
        v.id,
        CASE 
            WHEN (boc.isAvailable IS FALSE) THEN FALSE 
            WHEN (v.isDeleted IS TRUE) THEN FALSE
            ELSE TRUE 
        END,
        COALESCE(boc.priceOverride, v.recommendedPrice),
        v.name,
        ci.id
    )
    FROM CartItemVariant civ
    JOIN civ.optionVariant v
    JOIN civ.cartItem ci
    JOIN ci.cart c        
    LEFT JOIN BranchOptionConfig boc 
        ON boc.variant = v 
        AND boc.branch.id = c.restaurantBranch.id  
    WHERE ci.id IN :cartItemIds
""")
    List<VariantAvailabilityDTO> checkAvailabilityForCartItems(
            @Param("cartItemIds") List<Long> cartItemIds
    );
}