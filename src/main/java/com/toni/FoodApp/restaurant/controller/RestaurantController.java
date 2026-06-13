package com.toni.FoodApp.restaurant.controller;

import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.dtos.*;
import com.toni.FoodApp.restaurant.dtos.response.RestaurantDashboardDTO;
import com.toni.FoodApp.restaurant.dtos.response.RestaurantSummaryDTO;
import com.toni.FoodApp.restaurant.filterAndSpecification.BranchFilter;
import com.toni.FoodApp.restaurant.service.BranchService;
import com.toni.FoodApp.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping({"/api/restaurants", "/api/restaurants/"})
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final BranchService branchService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MANAGER')")
    public ResponseEntity<Response<?>> createRestaurant(
            @ModelAttribute RestaurantRequestDto restaurantDTO,
            @RequestParam("coverImage") MultipartFile coverImage,
            @RequestParam("profileImage") MultipartFile profileImage
    ) {
        restaurantDTO.setCoverImage(coverImage);
        restaurantDTO.setProfileImage(profileImage);
        return ResponseEntity.ok(restaurantService.createRestaurant(restaurantDTO));
    }

    @GetMapping("/all-restaurants")
    public ResponseEntity<Response<Page<RestaurantSummaryDTO>>> getAllRestaurants(
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {

        return ResponseEntity.ok(restaurantService.findAll(pageable));
    }

    @GetMapping("/all-restaurants-admin")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<Page<RestaurantsDto>>> getAllRestaurantsForAdmin(
            @PageableDefault(size = 10, page = 0) Pageable pageable,
            @RequestParam(name = "deleted", required = false, defaultValue = "false") Boolean deleted
    ) {

        return ResponseEntity.ok(restaurantService.findAllForAdmin(pageable,deleted));
    }

    @GetMapping("/available-restaurants-dashboard")
    public ResponseEntity<Response<RestaurantDashboardDTO>> getDashboardData(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {
        RestaurantFilterCriteria criteria = new RestaurantFilterCriteria();
        criteria.setMinRating(0.0);
        criteria.setIsFeatured(false);
        RestaurantFilterCriteria criteria1 = new RestaurantFilterCriteria();
        criteria1.setIsFeatured(false);
        Page<RestaurantSummaryDTO> normal = restaurantService.findAvailableRestaurants(criteria1,pageable,"default", lat, lng);
        Page<RestaurantSummaryDTO> topRated = Page.empty();
        Page<RestaurantSummaryDTO> fastest = Page.empty();
        Page<RestaurantSummaryDTO> trending = Page.empty();
        int totalBranches = normal.getContent()
                .stream()
                .mapToInt(dto -> dto.getBranches().size())
                .sum();
        if (totalBranches >=  10) {
            topRated  = restaurantService.findAvailableRestaurants(criteria,  pageable, null, lat, lng);
            fastest   = restaurantService.findAvailableRestaurants(criteria1, pageable, "delivery_time", lat, lng);
            trending  = restaurantService.findAvailableRestaurants(criteria1, pageable, "trending",lat, lng);
        }
        RestaurantDashboardDTO result = RestaurantDashboardDTO.builder()
                .fastest(fastest)
                .trending(trending)
                .topRated(topRated)
                .normal(normal)
                .build();

        return ResponseEntity.ok(
                Response.<RestaurantDashboardDTO>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Successfully fetched available venues.")
                        .data(result)
                        .build());
    }
    @GetMapping("/available-restaurants")
    public ResponseEntity<Response<Page<RestaurantSummaryDTO>>> findAvailableRestaurants(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @ModelAttribute RestaurantFilterCriteria criteria,
            @PageableDefault(size = 10, page = 0) Pageable pageable,
            @RequestParam(required = false, defaultValue = "default") String sort

    ) {
        return ResponseEntity.ok(
                Response.<Page<RestaurantSummaryDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Successfully fetched available venues.")
                .data(restaurantService.findAvailableRestaurants(criteria,pageable,sort,lat, lng))
                .build());
    }

    @GetMapping("/search-by-restaurant/{id}")
    public ResponseEntity<Response<RestaurantSummaryDTO>> findByRestaurantId(
            @PathVariable Long id
            ){
        return ResponseEntity.ok(restaurantService.findByRestaurant(id));
    }

    //--------Manager panel--------
    @PreAuthorize("hasAuthority('MANAGER')")
    @GetMapping("/manager/restaurant-branches")
    public ResponseEntity<Response<List<SimpleBranchDto>>> findBranchesForRestaurant(
            @ModelAttribute BranchFilter filter
    ) {
        return ResponseEntity.ok(
                restaurantService.findBranchesForRestaurant(filter)
        );
    }

    @PreAuthorize("hasAuthority('MANAGER')")
    @PutMapping("/manager/change-manager/{branchId}")
    public ResponseEntity<Response<?>> changeManager(@PathVariable Long branchId, @RequestBody(required = false) Long userId) {
        return ResponseEntity.ok(branchService.changeManager(branchId, userId));
    }


    @PreAuthorize("hasAuthority('MANAGER')")
    @DeleteMapping("/manager/delete-branch/{branchId}")
    public ResponseEntity<Response<?>> deleteBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(branchService.deleteBranch(branchId));
    }

    @PreAuthorize("hasAuthority('MANAGER')")
    @PutMapping("/manager/restore-branch/{branchId}")
    public ResponseEntity<Response<?>> restoreBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(branchService.restoreBranch(branchId));
    }

    @GetMapping("/restaurant")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<SimpleRestaurantDto>> getCurrentRestaurant() {
        return ResponseEntity.ok(
                restaurantService.getCurrentRestaurant()
        );
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/admin/delete-restaurant/{restaurantId}")
    public ResponseEntity<Response<?>> deleteRestaurant(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(restaurantService.deleteRestaurant(restaurantId));
    }
    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/admin/restore/{restaurantId}")
    public ResponseEntity<Response<?>> restoreRestaurant(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(restaurantService.restoreRestaurant(restaurantId));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/admin/unassign-restaurant/{restaurantId}")
    public ResponseEntity<Response<?>> unassignRestaurant(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(restaurantService.unassignRestaurant(restaurantId));
    }

}
