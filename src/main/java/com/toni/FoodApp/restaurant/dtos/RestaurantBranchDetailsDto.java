package com.toni.FoodApp.restaurant.dtos;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantBranchDetailsDto {

    private Long id;
    private String address;
    private String phoneNumber;
    private LocationDto location;
    private boolean isActive;
    private boolean isClosed;
    private Integer avgPrepTimeInMinutes;

    private Double deliveryRadiusInKm;
    private Integer minOrderAmount;

    private BigDecimal averageRating;
    private Integer reviewCount;

    private Long restaurantId;
    private String restaurantName;

    private String coverImageUrl;
    private String profileImageUrl;
    private List<OpeningHourDto> openingHours;
    private String desc;
@Data
@NoArgsConstructor
@AllArgsConstructor
public static class LocationDto {
    private double latitude;
    private double longitude;
}
}

