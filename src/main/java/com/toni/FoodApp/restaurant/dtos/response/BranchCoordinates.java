package com.toni.FoodApp.restaurant.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BranchCoordinates {
    private Double lat;
    private Double lng;
    private String restaurantName;
}
