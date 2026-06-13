package com.toni.FoodApp.restaurant.controller;

import com.toni.FoodApp.category.dtos.CategoryDTO;
import com.toni.FoodApp.menu.dtos.BranchManagerMenu;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.dtos.*;
import com.toni.FoodApp.restaurant.service.BranchService;
import com.toni.FoodApp.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/restaurant-branch", "/api/restaurant-branch/"})
@RequiredArgsConstructor
public class BranchController {
    private final RestaurantService restaurantService;
    private final BranchService branchService;

    @PutMapping("/edit-branch/{id}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<?> editBranch(
            @PathVariable Long id,
            @RequestBody BranchRequestDto dto) {
        return ResponseEntity.ok(branchService.editBranch(id,dto));
    }

    @PutMapping("/edit-branch/my-branch")
    @PreAuthorize("hasAuthority('BRANCH_MANAGER')")
    public ResponseEntity<?> editMyBranch(
            @RequestBody BranchOperationsRequest dto) {
        return ResponseEntity.ok(branchService.editMyBranch(dto));
    }


    @PutMapping("/myBranch/opening-hours")
    public ResponseEntity<?> saveOpeningHours(
            @RequestBody @Valid OpeningHoursRequestDto dto
    ) {
        return ResponseEntity.ok(branchService.saveOpeningHours(dto));
    }

    @PostMapping("/branch")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MANAGER')")
    public ResponseEntity<?> createBranch(@RequestBody BranchRequestDto branchDto) {
        return ResponseEntity.ok(branchService.createBranch(branchDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<RestaurantBranchDetailsDto>> getBranchById(@PathVariable Long id){
        return ResponseEntity.ok(branchService.getById(id));
    }

    @GetMapping("/{branchId}/menu")
    public ResponseEntity<Response<List<CategoryDTO>>> getMenus(
            @PathVariable Long branchId,
            @RequestParam(required = false) String searchString
            ){
        return ResponseEntity.ok(branchService.getMenusByBranchId(branchId, searchString));
    }
    @GetMapping("/myBranch")
    @PreAuthorize("hasAuthority('BRANCH_MANAGER')")
    public ResponseEntity<Response<RestaurantBranchDetailsDto>> getMyBranch(){
        return ResponseEntity.ok(branchService.getMyBranch());
    }

    @PutMapping("/change-opening-status")
    @PreAuthorize("hasAuthority('BRANCH_MANAGER')")
    public ResponseEntity<Response<Boolean>> changeOpeningStatus(){
        return ResponseEntity.ok(branchService.changeOpeningStatus());
    }

    @GetMapping("/location/{orderId}")
    @PreAuthorize("hasAuthority('DELIVERY')")
    public ResponseEntity<Response<BranchLocationDto>> getBranchLocation(@PathVariable Long orderId){
        return ResponseEntity.ok(branchService.getBranchLocation(orderId));
    }


}
