package com.toni.FoodApp.restaurant.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchRequestDto {
    private String address;
    private Long restaurantId;
    private boolean isActive = true;
    private double latitude;
    private double longitude;
    @JsonProperty("delivery_radius_in_km")
    private Double deliveryRadiusInKm;
}