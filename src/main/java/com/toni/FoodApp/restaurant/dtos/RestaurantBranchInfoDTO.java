package com.toni.FoodApp.restaurant.dtos;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RestaurantBranchInfoDTO {
    private Long id;
    private String name;
    private String address;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
}