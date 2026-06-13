package com.toni.FoodApp.restaurant.repository;

import com.toni.FoodApp.restaurant.entity.BranchMenuItem;
import com.toni.FoodApp.restaurant.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BranchMenuItemRepository extends JpaRepository<BranchMenuItem, Long> {
    List<BranchMenuItem> findByBranchId(Long branchId);

    Page<BranchMenuItem> findAllByBranchId(Long branchId, Pageable pageable);

    BranchMenuItem findByMenuIdAndBranchId(Long menuId, Long BranchId);
}
