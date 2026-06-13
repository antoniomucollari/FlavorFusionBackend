package com.toni.FoodApp.menu.controller;

import com.toni.FoodApp.menu.dtos.BranchManagerMenu;
import com.toni.FoodApp.menu.dtos.CreateBranchManagerMenu;
import com.toni.FoodApp.menu.dtos.MenuDTO;
import com.toni.FoodApp.menu.dtos.SimpleMenuDto;
import com.toni.FoodApp.menu.service.BranchManagerMenuServiceImpl;
import com.toni.FoodApp.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping({"/api/branch-manager/menu"})
@RequiredArgsConstructor
public class BranchManagerMenuController {
    private final BranchManagerMenuServiceImpl branchManagerMenuService;
    @GetMapping()
    @PreAuthorize("hasAuthority('BRANCH_MANAGER')")
    public ResponseEntity<Response<Page<BranchManagerMenu>>> getMenus(@PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ResponseEntity.ok(branchManagerMenuService.getMenus(pageable));
    }

    @PostMapping()
    @PreAuthorize("hasAuthority('BRANCH_MANAGER')")
    public ResponseEntity<Response<?>> createMenus(@ModelAttribute CreateBranchManagerMenu createBranchManagerMenu) {
        return ResponseEntity.ok(branchManagerMenuService.createMenu(createBranchManagerMenu));
    }
    @GetMapping("/restaurant")
    @PreAuthorize("hasAuthority('BRANCH_MANAGER')")
    public ResponseEntity<Response<List<SimpleMenuDto>>> getRestaurantMenus() {
        return ResponseEntity.ok(branchManagerMenuService.getRestaurantMenus());
    }

    @PutMapping()
    @PreAuthorize("hasAuthority('BRANCH_MANAGER')")
    public ResponseEntity<Response<?>> updateMenus(@ModelAttribute BranchMenuDto branchMenuDto) {
        return ResponseEntity.ok(branchManagerMenuService.updateMenu(branchMenuDto));
    }

    public record BranchMenuDto(Long id, BigDecimal price, Boolean available, Boolean highlighted) {}

}
