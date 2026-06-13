package com.toni.FoodApp.restaurant.dtos;

import lombok.Data;

@Data
public class BranchOperationsRequest {
    private Integer minOrderAmount;
    private Integer avgPrepTimeInMinutes;
    private String phoneNumber;
}
