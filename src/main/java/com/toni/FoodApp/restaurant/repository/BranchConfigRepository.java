package com.toni.FoodApp.restaurant.repository;

import com.toni.FoodApp.restaurant.entity.BranchOptionConfig;
import com.toni.FoodApp.restaurant.entity.OpeningHour;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface BranchConfigRepository extends JpaRepository<BranchOptionConfig, Long> {
    List<BranchOptionConfig> findByBranchId(Long id);
    Optional<BranchOptionConfig> findByVariantId(Long variantId);
    Optional<BranchOptionConfig> findByBranchIdAndVariantId(Long branchId, Long variantId);

    List<BranchOptionConfig> findByBranchIdAndVariantIdIn(Long branchId,List<Long> variantIds);

    Long branch(RestaurantBranch branch);
}
