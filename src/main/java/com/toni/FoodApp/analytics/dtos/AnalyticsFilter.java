package com.toni.FoodApp.analytics.dtos;

import lombok.Data;

@Data
public class AnalyticsFilter {
    private Long restaurantId;
    private Long branchId;
    private Long deliveryPersonId;
}