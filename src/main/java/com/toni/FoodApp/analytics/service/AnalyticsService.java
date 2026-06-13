package com.toni.FoodApp.analytics.service;

import com.toni.FoodApp.analytics.dtos.*;
import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.auth_users.services.UserService;
import com.toni.FoodApp.enums.OrderStatus;
import com.toni.FoodApp.enums.RoleName;
import com.toni.FoodApp.order.entity.Order;
import com.toni.FoodApp.order.repository.OrderItemRepository;
import com.toni.FoodApp.order.repository.OrderRepository;
import com.toni.FoodApp.order.specifications.OrderSpecifications;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.entity.Restaurant;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import com.toni.FoodApp.restaurant.repository.BranchRepository;
import com.toni.FoodApp.restaurant.service.BranchService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    private final UserService userService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BranchRepository branchRepository;

    public Response<List<BranchRevenueDto>> getBranchRevenue() {
        log.info("Inside getBranchRevenue()");
        // We delegate to the new location
        Restaurant restaurant = userService.getRestaurantIdByCurrentLoggedUser();
        List<RestaurantBranch> branches = getBranchesByRestaurantId(restaurant.getId());
        //Get branch revenues for this restaurant
        List<BranchRevenueDto> branchRevenueDTOS = new ArrayList<>();
        for (RestaurantBranch branch : branches) {

            double revenue = branch.getOrders()
                    .stream()
                    .map(Order::getTotalAmount)           // BigDecimal
                    .mapToDouble(BigDecimal::doubleValue) // Convert to double
                    .sum();

            BranchRevenueDto dto = BranchRevenueDto.builder()
                    .branchName(restaurant.getName() + " " + branch.getAddress())
                    .revenue(revenue)
                    .build();

            branchRevenueDTOS.add(dto);
        }

        return Response.<List<BranchRevenueDto>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Branch revenues retrieved successfully.")
                .data(branchRevenueDTOS)
                .build();
    }

    public Response<BigDecimal> getTotalRevenue() {
        // We delegate to the new location
        Restaurant restaurant = userService.getRestaurantIdByCurrentLoggedUser();
        List<RestaurantBranch> branches = getBranchesByRestaurantId(restaurant.getId());
        BigDecimal totalRevenue = branches.stream()
                .flatMap(branch -> branch.getOrders().stream())
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Response.<BigDecimal>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Total revenue retrieved successfully.")
                .data(totalRevenue)
                .build();
    }

    public Response<Long> getSuccessfulOrders() {
        // We delegate to the new location
        Restaurant restaurant = userService.getRestaurantIdByCurrentLoggedUser();
        List<RestaurantBranch> branches = getBranchesByRestaurantId(restaurant.getId());

        Long totalOrders = branches.stream()
                .flatMap(branch -> branch.getOrders().stream())
                .filter(order -> order.getOrderStatus() == OrderStatus.DELIVERED)
                .count();

        return Response.<Long>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Total delivered orders retrieved successfully.")
                .data(totalOrders)
                .build();
    }

    public List<RestaurantBranch> getBranchesByRestaurantId(Long restaurantId){
        return branchRepository.findByRestaurantId(restaurantId);
    }

    public CustomerStatsDto getStats(AnalyticsFilter filter) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);

        Specification<Order> baseFilterSpec = createRoleBasedSpecification(filter);

        Specification<Order> currentMonthSpec = baseFilterSpec.and(
                createdBetween(currentMonth.atDay(1), currentMonth.atEndOfMonth())
        );

        Specification<Order> previousMonthSpec = baseFilterSpec.and(
                createdBetween(previousMonth.atDay(1), previousMonth.atEndOfMonth())
        );

        long currentCount = orderRepository.count(currentMonthSpec);
        long previousCount = orderRepository.count(previousMonthSpec);

        // 4. Calculate Percentage
        double percentageDiff = calculateGrowth(currentCount, previousCount);

        // 5. Build Response
        return CustomerStatsDto.builder()
                .totalUniqueCustomers(currentCount)
                .currentMonthCustomers(currentCount)
                .previousMonthCustomers(previousCount)
                .percentageDifference(percentageDiff)
                .build();
    }

    private Specification<Order> createRoleBasedSpecification(AnalyticsFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // A. Scope Logic
            if (filter.getBranchId() != null) { //there is branch id
                // Specific Branch (Branch Manager)
                predicates.add(cb.equal(root.get("branch").get("id"), filter.getBranchId()));
            }
            else if (filter.getRestaurantId() != null) {
                // All Branches for this Restaurant (Manager)
                // Join Order -> Branch -> Restaurant
                predicates.add(cb.equal(root.get("branch").get("restaurant").get("id"), filter.getRestaurantId()));
            }
            predicates.add(cb.equal(root.get("orderStatus"), "DELIVERED"));


            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public List<PopularItemDto> getMostPopularItems(AnalyticsFilter filter, int limit) {
        Pageable pageLimit = PageRequest.of(0, limit);

        return orderItemRepository.findMostPopularMenus(
                filter.getRestaurantId(),
                filter.getBranchId(),
                pageLimit
        );
    }

    public List<MonthlyRevenueDto> getMonthlyRevenue(int year, AnalyticsFilter filter) {
        // 1. Fetch raw data from DB
        List<Object[]> rawData = orderRepository.getMonthlyRevenueByYear(
                year,
                filter.getRestaurantId(),
                filter.getBranchId()
        );

        // 2. Convert to Map for easy lookup
        Map<Integer, BigDecimal> revenueMap = rawData.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(), // Safely cast generic Number
                        row -> (BigDecimal) row[1]
                ));

        // 3. Fill in gaps (1-12 months)
        return calculateEarnings(revenueMap);
    }

    public List<DailyRevenueDto> getDailyRevenue(int year, int month, AnalyticsFilter filter) {
        // 1. Fetch raw data
        List<Object[]> rawData = orderRepository.getDailyRevenueForMonth(
                year,
                month,
                filter.getRestaurantId(),
                filter.getBranchId()
        );

        // 2. Convert to Map
        Map<Integer, BigDecimal> revenueMap = rawData.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> (BigDecimal) row[1]
                ));

        // 3. Calculate days in month dynamically
        int daysInMonth = java.time.YearMonth.of(year, month).lengthOfMonth();

        return IntStream.rangeClosed(1, daysInMonth)
                .mapToObj(dayNum -> {
                    BigDecimal revenue = revenueMap.getOrDefault(dayNum, BigDecimal.ZERO);
                    return new DailyRevenueDto(dayNum, revenue);
                })
                .collect(Collectors.toList());
    }

    /**
     * Helper to filter by Date Range
     */
    private Specification<Order> createdBetween(LocalDate start, LocalDate end) {
        return (root, query, cb) -> cb.between(root.get("orderDate"), start, end);
    }

    /**
     * Helper to calculate percentage safely
     */
    private double calculateGrowth(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((double) (current - previous) / previous) * 100;
    }


    public Response<Long> countAssignedOrdersInPreparation() {
        User currentUser = userService.getCurrentLoggedInUser();
        Long count = orderRepository.countByDeliveryPersonAndStillPreparing(currentUser.getId());
        return Response.<Long>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Total assigned orders in preparation retrieved successfully.")
                .data(count)
                .build();
    }

    public Response<List<MonthlyRevenueDto>> calculateEarnings(int year, Long deliveryId) {
        User currentUser = userService.getCurrentLoggedInUser();
        boolean isDelivery = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals(RoleName.DELIVERY));
        Long realDeliveryId = isDelivery ? currentUser.getId() : deliveryId;
        if (realDeliveryId == null) {throw new IllegalArgumentException("Delivery ID is required for admin role.");}

        List<Object[]> rawData = orderRepository.getMonthlyEarningsByYear(year, realDeliveryId);

        Map<Integer, BigDecimal> earningsMap = rawData.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO
                ));
        return Response.<List<MonthlyRevenueDto>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Driver earnings retrieved successfully.")
                .data(calculateEarnings(earningsMap))
                .build();
    }
    private List<MonthlyRevenueDto> calculateEarnings(Map<Integer, BigDecimal> earningsMap){
        return IntStream.rangeClosed(1, 12)
                .mapToObj(monthNum -> {
                    String monthName = Month.of(monthNum).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
                    BigDecimal earnings = earningsMap.getOrDefault(monthNum, BigDecimal.ZERO);
                    return new MonthlyRevenueDto(monthName, earnings);
                })
                .collect(Collectors.toList());
    }
}
