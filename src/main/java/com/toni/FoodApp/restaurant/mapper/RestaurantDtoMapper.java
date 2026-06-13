package com.toni.FoodApp.restaurant.mapper;

import com.toni.FoodApp.category.dtos.RestaurantCategoryDTO;
import com.toni.FoodApp.deliverTo.dtos.DeliveryInfo;
import com.toni.FoodApp.location.dto.DistanceMatrixInfo;
import com.toni.FoodApp.location.dto.RoutingProvider;
import com.toni.FoodApp.restaurant.dtos.BranchSummaryDto;
import com.toni.FoodApp.restaurant.dtos.response.RestaurantSummaryDTO;
import com.toni.FoodApp.restaurant.entity.Restaurant;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import com.toni.FoodApp.restaurant.repository.BranchRepository;
import com.toni.FoodApp.restaurant.repository.ReviewRepository;
import com.toni.FoodApp.restaurant.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class RestaurantDtoMapper {

    private final ModelMapper modelMapper;
    private final BranchRepository branchRepository;
    private final PricingService pricingService;
    private final ReviewRepository reviewRepository;

//    private final ReviewServiceImpl reviewService;

    private record BranchApiData(
            RestaurantBranch branch,
            DistanceMatrixInfo matrixInfo
    ) {
    }
    private record BranchWithDeliveryInfo(RestaurantBranch branch, DeliveryInfo deliveryInfo) {}
    /**
     * This is the main method that's been updated to use the Google Maps API.
     */

    public RestaurantSummaryDTO mapToSummaryDto(Restaurant restaurant, double userLatitude, double userLongitude, List<RestaurantBranch> branches) {
        RestaurantSummaryDTO dto = modelMapper.map(restaurant, RestaurantSummaryDTO.class);

        if (branches != null && !branches.isEmpty()) {

            Map<Long, DeliveryInfo> bulkDeliveryInfo = pricingService.calculateDeliveryInfoBulk(
                    branches, userLatitude, userLongitude, RoutingProvider.STANDARD
            );

            List<BranchSummaryDto> deliverableBranchDTOs = branches.stream()
                    .map(branch -> new BranchWithDeliveryInfo(
                            branch,
                            bulkDeliveryInfo.get(branch.getId())
                    ))
                    .filter(data -> data.deliveryInfo().isDeliverable())
                    .map(data -> {
                        RestaurantBranch branchEntity = data.branch();
                        DeliveryInfo deliveryInfo = data.deliveryInfo();

                        // Map to DTO
                        BranchSummaryDto branchDto = modelMapper.map(branchEntity, BranchSummaryDto.class);
                        branchDto.setLatitude(branchEntity.getLocation().getY());
                        branchDto.setLongitude(branchEntity.getLocation().getX());

                        // Set rating/min order
                        branchDto.setRating(branchEntity.getAverageRating());
                        branchDto.setRoundedReviewCount(roundReviewCount(branchEntity.getReviewCount()));
                        branchDto.setMinOrderAmount(branchEntity.getMinOrderAmount());

                        // --- Set fields from the REUSABLE DTO ---
                        branchDto.setDeliveryPrice(deliveryInfo.getDeliveryFee());
                        branchDto.setDeliveryTime(deliveryInfo.getDeliveryTime());
                        branchDto.setDistanceInKm(deliveryInfo.getDistanceInKm());
                        branchDto.setIsTrending(branchEntity.getDailyOrderCount() > 10);

                        return branchDto;
                    })
                    .collect(Collectors.toList());

            dto.setBranches(deliverableBranchDTOs);
        } else {
            dto.setBranches(Collections.emptyList());
        }

        // ... (category mapping remains the same) ...
        return dto;
    }
    private int roundReviewCount(int count) {
        if (count < 10) {
            return count;
        } else if (count < 100) {
            return (int) (Math.round(count / 10.0) * 10);
        } else {
            return (int) (Math.round(count / 100.0) * 100);
        }
    }
    public RestaurantSummaryDTO mapToSummaryDto(Restaurant restaurant) {
        RestaurantSummaryDTO dto = modelMapper.map(restaurant, RestaurantSummaryDTO.class);

        if (restaurant.getCategories() != null) {
            List<RestaurantCategoryDTO> categoryDTOs = restaurant.getCategories().stream()
                    .map(categoryEntity -> modelMapper.map(categoryEntity, RestaurantCategoryDTO.class))
                    .collect(Collectors.toList());
            dto.setCategories(categoryDTOs);
        } else {
            dto.setCategories(Collections.emptyList());
        }

        List<RestaurantBranch> branches = branchRepository.findByRestaurantId(restaurant.getId());

        if (branches != null && !branches.isEmpty()) {
            List<BranchSummaryDto> branchDTOs = branches.stream()
                    .map(branchEntity -> {
                        BranchSummaryDto branchDto = modelMapper.map(branchEntity, BranchSummaryDto.class);
                        if (branchEntity.getLocation() != null) {
                            Point location = branchEntity.getLocation();
                            branchDto.setLatitude(location.getY());
                            branchDto.setLongitude(location.getX());
                        }
                        return branchDto;
                    })
                    .collect(Collectors.toList());
            dto.setBranches(branchDTOs);
        } else {
            dto.setBranches(Collections.emptyList());
        }

        return dto;
    }

//    private int getMinutesUntilClose(RestaurantBranch branch) {
//        LocalDateTime now = LocalDateTime.now(); // Assumes server timezone
//        DayOfWeek today = now.getDayOfWeek();
//        LocalTime currentTime = now.toLocalTime();
//
//        Optional<OpeningHour> todayHoursOpt = branch.getOpeningHours().stream()
//                .filter(oh -> oh.getDayOfWeek() == today)
//                .findFirst();
//
//        if (todayHoursOpt.isEmpty()) {
//            return -1; // No schedule for today
//        }
//
//        OpeningHour todayHours = todayHoursOpt.get();
//
//        if (todayHours.getOpenTime() == null || todayHours.getCloseTime() == null) {
//            return -1; // No schedule defined for today
//        }
//
//        LocalTime openTime = todayHours.getOpenTime();
//        LocalTime closeTime = todayHours.getCloseTime();
//
//        boolean isOvernight = closeTime.isBefore(openTime);
//
//        if (isOvernight) {
//            if (currentTime.isAfter(openTime) || currentTime.isBefore(closeTime)) {
//                if (currentTime.isAfter(openTime)) {
//                    long minsToMidnight = ChronoUnit.MINUTES.between(currentTime, LocalTime.MAX) + 1;
//                    long minsFromMidnight = ChronoUnit.MINUTES.between(LocalTime.MIN, closeTime);
//                    return (int) (minsToMidnight + minsFromMidnight);
//                } else {
//                    return (int) ChronoUnit.MINUTES.between(currentTime, closeTime);
//                }
//            }
//        } else {
//            if (currentTime.isAfter(openTime) && currentTime.isBefore(closeTime)) {
//                return (int) ChronoUnit.MINUTES.between(currentTime, closeTime);
//            }
//        }
//        return -1;
//    }
}