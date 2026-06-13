package com.toni.FoodApp.restaurant.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchSummaryDto {
    private Long id;
    private String address;
    private String phoneNumber;
    private boolean isActive = true;
    private double latitude;
    private double longitude;
    private double distanceInKm;
    private String deliveryTime;
    private BigDecimal deliveryPrice;
    private BigDecimal rating;
    private Integer roundedReviewCount;
    private Integer minOrderAmount;
    private Boolean isTrending;
}