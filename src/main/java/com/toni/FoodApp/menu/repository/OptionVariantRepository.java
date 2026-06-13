package com.toni.FoodApp.menu.repository;

import com.toni.FoodApp.cart.dtos.VariantAvailabilityDTO;
import com.toni.FoodApp.menu.dtos.VariantWithPriceDTO;
import com.toni.FoodApp.menu.entity.OptionGroup;
import com.toni.FoodApp.menu.entity.OptionVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface OptionVariantRepository extends JpaRepository<OptionVariant, Long> {

    @Query("""
    SELECT new com.toni.FoodApp.menu.dtos.VariantWithPriceDTO(
        v, 
        COALESCE(boc.priceOverride, v.recommendedPrice)
    )
    FROM OptionVariant v
    LEFT JOIN BranchOptionConfig boc 
        ON boc.variant = v AND boc.branch.id = :branchId
    WHERE v.id IN :variantIds
""")
    List<VariantWithPriceDTO> findVariantsWithEffectivePrice(
            @Param("variantIds") Set<Long> variantIds,
            @Param("branchId") Long branchId
    );

    @Query("""
    SELECT new com.toni.FoodApp.cart.dtos.VariantAvailabilityDTO(
        v.id,
        CASE 
            WHEN (boc.isAvailable IS FALSE) THEN FALSE 
            WHEN (boc.id IS NULL AND v.isDeleted IS TRUE) THEN FALSE
            ELSE TRUE 
        END,
        COALESCE(boc.priceOverride, v.recommendedPrice)
    )
    FROM OptionVariant v
    LEFT JOIN BranchOptionConfig boc 
        ON boc.variant = v AND boc.branch.id = :branchId
    WHERE v.id IN :variantIds
""")
    List<VariantAvailabilityDTO> getAvailabilityStatus(
            @Param("variantIds") Set<Long> ids,
            @Param("branchId") Long branchId
    );




}
