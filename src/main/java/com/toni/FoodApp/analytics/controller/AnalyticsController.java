package com.toni.FoodApp.analytics.controller;

import com.toni.FoodApp.analytics.dtos.*;
import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.auth_users.services.UserService;
import com.toni.FoodApp.enums.RoleName;
import com.toni.FoodApp.exceptions.RestaurantMissingException;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.analytics.service.AnalyticsService;
import com.toni.FoodApp.role.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/analytics"})
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final UserService userService;

    @GetMapping()
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<List<BranchRevenueDto>>> getBranchRevenue(){
        return ResponseEntity.ok(analyticsService.getBranchRevenue());
    }

    @GetMapping("/total-revenue")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<?>> getTotalRevenue(){
        return ResponseEntity.ok(analyticsService.getTotalRevenue());
    }

    @GetMapping("/successful-orders")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<?>> getTotalOrders(){
        return ResponseEntity.ok(analyticsService.getSuccessfulOrders());
    }

    @GetMapping("/unique-customer-metrics")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MANAGER') or hasAuthority('BRANCH_MANAGER')")
    public ResponseEntity<CustomerStatsDto> getUniqueCustomerMetrics(
            @RequestParam(required = false) Long targetBranchId,
            @RequestParam(required = false) Long targetRestaurantId // Added for Admin
    ) {
        User currentUser = userService.getCurrentLoggedInUser();
        AnalyticsFilter filter = buildFilter(currentUser, targetBranchId, targetRestaurantId);
        return ResponseEntity.ok(analyticsService.getStats(filter));
    }

    @GetMapping("/popular-items")
    public ResponseEntity<List<PopularItemDto>> getMostPopularItems(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) Long targetBranchId,
            @RequestParam(required = false) Long targetRestaurantId
    ) {
        User currentUser = userService.getCurrentLoggedInUser();
        AnalyticsFilter filter = buildFilter(currentUser, targetBranchId, targetRestaurantId);
        return ResponseEntity.ok(analyticsService.getMostPopularItems(filter, limit));
    }

    @GetMapping("/revenue/monthly")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MANAGER') or hasAuthority('BRANCH_MANAGER')")
    public ResponseEntity<Response<List<MonthlyRevenueDto>>> getMonthlyRevenue(
            @RequestParam int year,
            @RequestParam(required = false) Long targetBranchId,
            @RequestParam(required = false) Long targetRestaurantId
    ) {
        User currentUser = userService.getCurrentLoggedInUser();
        AnalyticsFilter filter = buildFilter(currentUser, targetBranchId, targetRestaurantId);

        List<MonthlyRevenueDto> data = analyticsService.getMonthlyRevenue(year, filter);

        return ResponseEntity.ok(Response.<List<MonthlyRevenueDto>>builder()
                .statusCode(200)
                .message("Monthly revenue fetched successfully.")
                .data(data)
                .build());
    }

    @GetMapping("/revenue/daily")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MANAGER') or hasAuthority('BRANCH_MANAGER')")
    public ResponseEntity<Response<List<DailyRevenueDto>>> getDailyRevenue(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Long targetBranchId,
            @RequestParam(required = false) Long targetRestaurantId
    ) {
        User currentUser = userService.getCurrentLoggedInUser();
        AnalyticsFilter filter = buildFilter(currentUser, targetBranchId, targetRestaurantId);

        List<DailyRevenueDto> data = analyticsService.getDailyRevenue(year, month, filter);

        return ResponseEntity.ok(Response.<List<DailyRevenueDto>>builder()
                .statusCode(200)
                .message("Daily revenue fetched successfully.")
                .data(data)
                .build());
    }
    @PreAuthorize("hasAuthority('DELIVERY')")
    @GetMapping("/underPrepare/delivery")
    public ResponseEntity<Response<Long>> countAssignedOrdersInPreparation(){
        return ResponseEntity.ok(analyticsService.countAssignedOrdersInPreparation());
    }

    //helper method
    private AnalyticsFilter buildFilter(User user, Long branchIdParam, Long restaurantIdParam) {
        AnalyticsFilter filter = new AnalyticsFilter();

        // Get the role string safely
        RoleName role = user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElseThrow(() -> new AccessDeniedException("User has no role"));

        switch (role) {
            case RoleName.ADMIN:
                // Admin sees whatever they ask for
                filter.setBranchId(branchIdParam);
                filter.setRestaurantId(restaurantIdParam);
                break;

            case RoleName.MANAGER:
                if (user.getRestaurant() == null) {
                    throw new RestaurantMissingException("This Manager user is not linked to any Restaurant!");
                }

                // 2. Force the Scope to their Restaurant
                filter.setRestaurantId(user.getRestaurant().getId());

                // 3. Check if the requested branch belongs to their restaurant
                if (branchIdParam != null) {
                    // Security Check: Does this branch belong to my restaurant?
                    boolean ownsBranch = user.getManagedBranch().getId().equals(branchIdParam);

                    if (!ownsBranch) {
                        throw new AccessDeniedException("You cannot view stats for a branch you do not own.");
                    }
                    filter.setBranchId(branchIdParam);
                }
                break;

            case RoleName.BRANCH_MANAGER:
                if (user.getManagedBranch() == null) {
                    throw new RuntimeException("This Branch Manager is not assigned to any Branch!");
                }

                // 2. Force the Scope to their specific Branch ONLY
                filter.setBranchId(user.getManagedBranch().getId());
                break;

            case RoleName.DELIVERY:
                throw new AccessDeniedException("Delivery personnel cannot access restaurant analytics.");

            default:
                throw new AccessDeniedException("Role not authorized");
        }

        return filter;
    }
    @PreAuthorize("hasAuthority('DELIVERY') or hasAuthority('ADMIN')")
    @GetMapping("/earnings")
    public ResponseEntity<Response<List<MonthlyRevenueDto>>> calculateEarnings(
            @RequestParam int year,
            @RequestParam(required = false) Long deliveryId){
        return ResponseEntity.ok(analyticsService.calculateEarnings(year, deliveryId));
    }

}