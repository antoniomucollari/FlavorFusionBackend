package com.toni.FoodApp.restaurant.mapper;
import com.toni.FoodApp.restaurant.dtos.OpeningHourDto;
import com.toni.FoodApp.restaurant.dtos.RestaurantBranchDetailsDto;
import com.toni.FoodApp.restaurant.dtos.RestaurantBranchInfoDTO;
import com.toni.FoodApp.restaurant.entity.OpeningHour;
import com.toni.FoodApp.restaurant.entity.Restaurant;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import com.toni.FoodApp.restaurant.repository.BranchRepository;
import org.springframework.stereotype.Component;
import org.locationtech.jts.geom.Point; // Make sure this is your Point import

import java.util.Collections;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter; // For formatting times

@Component
public class RestaurantBranchMapper {

    // Formatter for "HH:mm" format (e.g., "09:00")
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final BranchRepository branchRepository;

    public RestaurantBranchMapper(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    /**
     * Main method to map the Entity to the Details DTO.
     */
    public RestaurantBranchDetailsDto mapToBranchDetailsDto(RestaurantBranch branch) {
        if (branch == null) {
            return null;
        }
        Restaurant restaurant = branch.getRestaurant();

        // Use the DTO's builder for a clean construction
        return RestaurantBranchDetailsDto.builder()
                .id(branch.getId())
                .desc(restaurant.getDescription())
                .address(branch.getAddress())
                .coverImageUrl(restaurant.getCoverImageUrl())
                .profileImageUrl(restaurant.getProfileImageUrl())
                .phoneNumber(branch.getPhoneNumber())
                .location(mapPointToLocationDto(branch.getLocation()))
                .isActive(branch.isActive())
                .isClosed(branch.isClosed())
                .avgPrepTimeInMinutes(branch.getAvgPrepTimeInMinutes())
                .deliveryRadiusInKm(branch.getDeliveryRadiusInKm())
                .minOrderAmount(branch.getMinOrderAmount())
                // --- Your specific fields ---
                .averageRating(branch.getAverageRating())
                .reviewCount(branch.getReviewCount())
                .restaurantId(branch.getRestaurant() != null ? branch.getRestaurant().getId() : null)
                .restaurantName(branch.getRestaurant() != null ? branch.getRestaurant().getName() : null) // Assuming Restaurant has getName()

                .openingHours(
                        branch.getOpeningHours() != null ?
                                branch.getOpeningHours().stream()
                                        .map(this::mapToOpeningHourDto) // <-- Helper method
                                        .collect(Collectors.toList())
                                : Collections.emptyList()
                )
                .build();
    }

    /**
     * Helper to convert a JTS Point to nested LocationDto.
     * It handles the "location": null case.
     */
    private RestaurantBranchDetailsDto.LocationDto mapPointToLocationDto(Point point) {
        if (point == null) {
            return null;
        }

        return new RestaurantBranchDetailsDto.LocationDto(point.getY(), point.getX());
    }

    /**
     * Helper to map an OpeningHour entity to its DTO.
     */
    private OpeningHourDto mapToOpeningHourDto(OpeningHour openingHour) {
        if (openingHour == null) {
            return null;
        }

        return OpeningHourDto.builder()
                .dayOfWeek(openingHour.getDayOfWeek())
                .openTime(openingHour.getOpenTime())
                .closeTime(openingHour.getCloseTime())
                .build();
    }

    public RestaurantBranchInfoDTO toInfoDTO(Long branchId) {
        RestaurantBranch branch =  branchRepository.findById(branchId).orElse(null);
        if (branch == null) {
            return null;
        }
        return RestaurantBranchInfoDTO.builder()
                .id(branch.getId())
                .name(branch.getRestaurant().getName())
                .address(branch.getAddress())
                .imageUrl(branch.getRestaurant().getProfileImageUrl())
                .latitude(branch.getLocation().getY())
                .longitude(branch.getLocation().getX())
                .build();
    }


}